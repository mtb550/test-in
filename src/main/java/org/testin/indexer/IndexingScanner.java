package org.testin.indexer;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.DirectoryMapper;
import org.testin.model.DirectoryType;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.*;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@AllArgsConstructor
final class IndexingScanner {

    /**
     * The one key left of the order format this build no longer reads. Quoted,
     * so it is the JSON field rather than the word.
     */
    private static final @NotNull String LEGACY_ORDER_KEY = "\"isHead\"";

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
            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            if (tp.getMarker().getStatus() == ProjectStatus.ARCHIVED) {
                Logger.info("Skipping archived project: " + projectPath.getFileName());
                return;
            }

            store.getTestProjectsByPath().put(projectPath.toString(), tp);

                indicator.setFraction(0.1);
                indicator.setText(tp.getName() + " - test sets...");

            final @NotNull TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            store.getTestCasesMainDirsByPath().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator);
            reportUnreadableOrder(tp);

                indicator.setFraction(0.5);
                indicator.setText(tp.getName() + " - test runs...");

            final @NotNull TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            store.getTestRunsMainDirsByPath().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator);

                indicator.setFraction(1.0);
                indicator.setText(tp.getName() + " - done.");

        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    /**
     * Says so, once per project, when this build cannot read the order the
     * project was written in (#92).
     * <p>
     * Order used to be a chain across the files - {@code isHead} on one case,
     * {@code next} on the rest - and 2.8 and 2.9 converted a set the first time
     * they indexed it. 2.10 deleted that converter (#91), because by then every
     * machine that had opened a project had run it.
     * <p>
     * Every machine that had opened one. A tester who was on 2.8 when 2.10
     * arrived, or who installs fresh over an old checkout, or who clones a
     * colleague's repository nobody opened in 2.9, still has the chain - and
     * nothing about that fails loudly. The cases all show, in creation order,
     * which reads as the plugin having shuffled a set somebody arranged by hand.
     * <p>
     * So it is said instead. The question is asked of the data rather than of the
     * machine's install history, because a repository carries its files onto
     * whatever machine clones it, however up to date that machine is.
     * <p>
     * One notification per project: a tester with fifty unconverted sets has one
     * problem, not fifty.
     */
    private void reportUnreadableOrder(final @NotNull TestProjectDirectoryDto tp) {
        final boolean unreadable = store.getTestCasesById().values().stream()
                .filter(tc -> tc.getOrder().isEmpty())
                .filter(tc -> tc.getParent().getPath().startsWith(tp.getPath()))
                .anyMatch(IndexingScanner::carriesLegacyChain);

        if (!unreadable) return;

        Services.getInstance(p, Notifier.class).warn(p, "Test Cases Written by an Older Format",
                tp.getName() + " was last written by Testin 2.8 or earlier, so the order of its test cases cannot be read and they are shown oldest first."
                        + " Install Testin 2.9 once to convert them, then update again.");
    }

    /**
     * Whether this case's file still carries the old chain.
     * <p>
     * Read as text, because the model dropped both keys - so the file is the only
     * thing left that can answer. Only a case with no rank is ever read again,
     * which is none at all in a project that has been through 2.9, and a case
     * that arrives unranked for an ordinary reason - copied in by hand, imported
     * by another tool - costs one read and answers no.
     */
    private static boolean carriesLegacyChain(final @NotNull TestCaseDto tc) {
        try {
            return Files.readString(tc.getParent().getPath().resolve(tc.getId() + ".json"), StandardCharsets.UTF_8).contains(LEGACY_ORDER_KEY);
        } catch (final Exception unreadable) {
            return false;
        }
    }

    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // Between test sets, because that is where the tester's Cancel
                // has to land: a project is thousands of files and the pass is
                // long enough to want stopping. Asked rather than thrown -
                // stopping is an answer, so there is no exception for every
                // caller above to sort back out from a real failure.
                if (indicator.isCanceled()) return;

                if (Files.exists(dirPath.resolve(DirectoryType.TS.getMarker()))) {
                    scanTestSet(dirPath, parent, indicator);

                } else if (Files.exists(dirPath.resolve(DirectoryType.TSP.getMarker()))) {
                    scanTestSetPackage(dirPath, parent, indicator);

                } else {
                    Logger.warn("Skipping unmarked directory under test cases (missing .ts/.tsp): " + dirPath);
                }
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test sets: " + ex.getMessage());
        }
    }

    private void scanTestSetPackage(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestSetPackageDirectoryDto tsp = dirMapper.getTestSetPackageNode(p, path, parent);

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

    private void scanTestSet(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestSetDirectoryDto ts = dirMapper.getTestSetNode(p, path, parent);

            store.getTestSetsDirByPath().put(path.toString(), ts);

            final @NotNull List<UUID> caseIds = Collections.synchronizedList(new ArrayList<>());
            final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final @NotNull TestCaseDto tc = mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                tc.setParent(ts);
                                tc.setId(identityOf(filePath, tc));
                                store.getTestCasesById().put(tc.getId(), tc);
                                caseIds.add(tc.getId());
                            } catch (final Exception ex) {
                                Logger.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + ex.getMessage());
                            }
                        });
            }

            store.getTestSetCaseIds().put(path.toString(), caseIds);

            indicator.setText("Test set: " + ts.getName() + " (" + caseIds.size() + " cases)");

        } catch (final Exception ex) {
            Logger.error("Failed to scan test set '" +
                    path.getFileName().toString() + "': " + ex.getMessage());
        }
    }

    private void scanTestRunDirs(final @NotNull Path trDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // The same stopping point on the run side, for the same reason.
                if (indicator.isCanceled()) return;

                if (Files.exists(dirPath.resolve(DirectoryType.TR.getMarker()))) {
                    scanTestRun(dirPath, parent, indicator);
                } else if (Files.exists(dirPath.resolve(DirectoryType.TRP.getMarker()))) {
                    scanTestRunPackageDir(dirPath, parent, indicator);
                } else {
                    Logger.warn("Skipping unmarked directory under test runs (missing .tr/.trp): " + dirPath);
                }
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test runs: " + ex.getMessage());
        }
    }

    private void scanTestRunPackageDir(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunPackageDirectoryDto trp = dirMapper.getTestRunPackageNode(p, path, parent);

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

    private void scanTestRun(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunDirectoryDto tr = dirMapper.getTestRunNode(p, path, parent);

            store.getTestRunsDirByPath().put(path.toString(), tr);

            final @NotNull String fileName = path.getFileName().toString();
            final @NotNull Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
                final @NotNull TestRunDto trr = mapper.readValue(jsonPath.toFile(), TestRunDto.class);
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
        final @NotNull String name = filePath.getFileName().toString().replace(".json", "");

        try {
            final @NotNull UUID fromName = UUID.fromString(name);

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
