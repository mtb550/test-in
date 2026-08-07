package org.testin.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.enums.ProjectStatus;
import org.testin.logger.Logger;
import org.testin.mappers.DirectoryMapper;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.*;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

final class IndexingScanner {

    private final @NotNull Project p;
    private final IndexerDataStore store;

    IndexingScanner(final @NotNull Project p, final @NotNull IndexerDataStore store) {
        this.p = p;
        this.store = store;
    }

    void scanProject(final Path projectPath, final ProgressIndicator indicator) {
        scanProjectContents(projectPath, indicator);
    }

    void scanProject(final Path projectPath) {
        scanProjectContents(projectPath, null);
    }

    private void scanProjectContents(final Path projectPath, final ProgressIndicator indicator) {
        try {
            final TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            if (tp.getMarker().getStatus() == ProjectStatus.ARCHIVED) {
                Logger.info("Skipping archived project: " + projectPath.getFileName());
                return;
            }

            store.getTestProjectsByPath().put(projectPath.toString(), tp);

            if (indicator != null) {
                indicator.setFraction(0.1);
                indicator.setText(tp.getName() + " - test sets...");
            }

            final TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            store.getTestCasesMainDirsByPath().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator);


            if (indicator != null) {
                indicator.setFraction(0.5);
                indicator.setText(tp.getName() + " - test runs...");
            }

            final TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            store.getTestRunsMainDirsByPath().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator);

            if (indicator != null) {
                indicator.setFraction(1.0);
                indicator.setText(tp.getName() + " - done.");
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    private void scanTestSets(final Path tcDir, final DirectoryDto parent, final ProgressIndicator indicator) {
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

    private void scanTestSetPackage(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
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

    private void scanTestSet(final Path path, final DirectoryDto parent,
                             final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestSetDirectoryDto ts = dirMapper.getTestSetNode(p, path, parent);

            store.getTestSetsDirByPath().put(path.toString(), ts);

            final List<UUID> caseIds = Collections.synchronizedList(new ArrayList<>());
            final Mapper mapper = Services.getInstance(p, Mapper.class);

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                tc.setParent(ts);
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
                    (path != null ? path.getFileName().toString() : "null") + "': " + ex.getMessage());
        }
    }

    private void scanTestRunDirs(final Path trDir, final DirectoryDto parent,
                                 final ProgressIndicator indicator) {
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

    private void scanTestRunPackageDir(final Path path, final DirectoryDto parent,
                                       final ProgressIndicator indicator) {
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

    private void scanTestRun(final Path path, final DirectoryDto parent,
                             final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final TestRunDirectoryDto tr = dirMapper.getTestRunNode(p, path, parent);

            store.getTestRunsDirByPath().put(path.toString(), tr);

            final String fileName = path.getFileName().toString();
            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final Mapper mapper = Services.getInstance(p, Mapper.class);
                final TestRunDto trr = mapper.readValue(jsonPath.toFile(), TestRunDto.class);
                store.getTestRunsByPath().put(path.toString(), trr);
            }

            indicator.setText("Test run: " + fileName);

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run '" +
                    (path != null ? path.getFileName().toString() : "null") + "': " + ex.getMessage());
        }
    }
}
