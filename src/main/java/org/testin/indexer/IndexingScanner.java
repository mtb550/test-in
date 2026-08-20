package org.testin.indexer;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.DirectoryType;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.*;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Stream;

@AllArgsConstructor
final class IndexingScanner {

    private final @NotNull Project p;
    private final @NotNull IndexerDataStore store;

    void scanProject(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            scanProjectContents(projectPath, indicator);
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    void scanProject(final @NotNull Path projectPath) {
        try {
            scanProjectContents(projectPath, new EmptyProgressIndicator());
        } finally {
            store.invalidateChildrenIndex();
        }
    }

    private void scanProjectContents(final @NotNull Path projectPath, final @NotNull ProgressIndicator indicator) {
        try {
            final TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            if (tp.getMarker().getStatus() == ProjectStatus.ARCHIVED) {
                Logger.info("Skipping archived project: " + projectPath.getFileName());
                return;
            }

            store.getTestProjectsByPath().put(projectPath.toString(), tp);

                indicator.setFraction(0.1);
                indicator.setText(tp.getName() + " - test sets...");

            final TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            store.getTestCasesMainDirsByPath().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator);


                indicator.setFraction(0.5);
                indicator.setText(tp.getName() + " - test runs...");

            final TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            store.getTestRunsMainDirsByPath().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator);

                indicator.setFraction(1.0);
                indicator.setText(tp.getName() + " - done.");

        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final List<Path> dirs = paths.filter(Files::isDirectory).toList();

            dirs.forEach(dirPath -> {
                if (Files.exists(dirPath.resolve(DirectoryType.TS.getMarker()))) {
                    scanTestSet(dirPath, parent, indicator);

                } else if (Files.exists(dirPath.resolve(DirectoryType.TSP.getMarker()))) {
                    scanTestSetPackage(dirPath, parent, indicator);

                } else {
                    Logger.warn("Skipping unmarked directory under test cases (missing .ts/.tsp): " + dirPath);
                }
            });
        } catch (final Exception ex) {
            Logger.error("Failed to list test sets: " + ex.getMessage());
        }
    }

    private void scanTestSetPackage(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestSetPackageDirectoryDto tsp = dirMapper.getTestSetPackageNode(p, path, parent);

            store.getTestSetPackagesByPath().put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TS.getMarker()))) {
                                scanTestSet(subPath, tsp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TSP.getMarker()))) {
                                scanTestSetPackage(subPath, tsp, indicator);
                            }
                        });
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test set package: " + path.getFileName());
        }
    }

    private void scanTestSet(final @NotNull Path path, final @NotNull DirectoryDto parent,
                             final @NotNull ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestSetDirectoryDto ts = dirMapper.getTestSetNode(p, path, parent);

            store.getTestSetsDirByPath().put(path.toString(), ts);

            final List<UUID> caseIds = Collections.synchronizedList(new ArrayList<>());
            final Map<Path, TestCaseDto> loaded = Collections.synchronizedMap(new LinkedHashMap<>());
            final Mapper mapper = Services.getInstance(p, Mapper.class);

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                tc.setParent(ts);
                                tc.setId(identityOf(filePath, tc));
                                store.getTestCasesById().put(tc.getId(), tc);
                                caseIds.add(tc.getId());
                                loaded.put(filePath, tc);
                            } catch (final Exception ex) {
                                Logger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + ex.getMessage());
                            }
                        });
            }

            store.getTestSetCaseIds().put(path.toString(), caseIds);

            // A set written before cases carried their own rank has its order in
            // the old chain, and only here can it still be read - the model has
            // dropped those keys. Converted once and written back, so the tester
            // finds their set in the order they arranged it.
            for (final TestCaseDto converted : LegacyChainOrder.apply(p, loaded)) {
                store.putTestCase(path, converted);
            }
                indicator.setText("Test set: " + ts.getName() + " (" + caseIds.size() + " cases)");

        } catch (final Exception ex) {
            Logger.error("Failed to scan test set '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    private void scanTestRunDirs(final @NotNull Path trDir, final @NotNull DirectoryDto parent,
                                 final @NotNull ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final List<Path> dirs = paths.filter(Files::isDirectory).toList();

            dirs.forEach(dirPath -> {
                if (Files.exists(dirPath.resolve(DirectoryType.TR.getMarker()))) {
                    scanTestRun(dirPath, parent, indicator);
                } else if (Files.exists(dirPath.resolve(DirectoryType.TRP.getMarker()))) {
                    scanTestRunPackageDir(dirPath, parent, indicator);
                } else {
                    Logger.warn("Skipping unmarked directory under test runs (missing .tr/.trp): " + dirPath);
                }
            });
        } catch (final Exception ex) {
            Logger.error("Failed to list test runs: " + ex.getMessage());
        }
    }

    private void scanTestRunPackageDir(final @NotNull Path path, final @NotNull DirectoryDto parent,
                                       final @NotNull ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestRunPackageDirectoryDto trp = dirMapper.getTestRunPackageNode(p, path, parent);

            store.getTestRunPackagesByPath().put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TR.getMarker()))) {
                                scanTestRun(subPath, trp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TRP.getMarker()))) {
                                scanTestRunPackageDir(subPath, trp, indicator);
                            }
                        });
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run package: " + path.getFileName());
        }
    }

    private void scanTestRun(final @NotNull Path path, final @NotNull DirectoryDto parent,
                             final @NotNull ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestRunDirectoryDto tr = dirMapper.getTestRunNode(p, path, parent);

            store.getTestRunsDirByPath().put(path.toString(), tr);

            final String fileName = path.getFileName().toString();
            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final Mapper mapper = Services.getInstance(p, Mapper.class);
                final TestRunDto trr = mapper.readValue(jsonPath.toFile(), TestRunDto.class);
                trr.dropStampsWithoutVerdict();
                store.getTestRunsByPath().put(path.toString(), trr);
            }

                indicator.setText("Test run: " + fileName);

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    /**
     * Which test case a file is: its name, when the name is a UUID.
     * <p>
     * The plugin writes a case to {@code <id>.json} and reads it back keyed by
     * the id inside, so the two always agree - until a file is copied outside
     * the plugin, which is a thing people do on GitHub. Then two files claim one
     * id, the cache keeps whichever the parallel scan reached last, and the other
     * case is gone. Worse than gone: when the file that lost was the one holding
     * {@code isHead}, the set has no starting point at all and every case in it
     * shows as unsorted.
     * <p>
     * The name is the identity because it cannot collide - one directory cannot
     * hold two files with the same name - so a copied file becomes a second case
     * rather than a coin toss. It arrives pointed at by nothing, which is what
     * the Unsorted badge is for.
     * <p>
     * A name that is not a UUID keeps the id inside the file: that is a file the
     * plugin did not write, and inventing an identity for it would be worse than
     * believing what it says.
     */
    private static @NotNull UUID identityOf(final @NotNull Path filePath, final @NotNull TestCaseDto tc) {
        final String name = filePath.getFileName().toString().replace(".json", "");

        try {
            final UUID fromName = UUID.fromString(name);

            if (!fromName.equals(tc.getId())) {
                Logger.warn("Test case " + filePath.getFileName() + " says its id is " + tc.getId()
                        + "; the file name is the identity, so it is read as " + fromName);
            }
            return fromName;

        } catch (final IllegalArgumentException ex) {
            return tc.getId();
        }
    }
}
