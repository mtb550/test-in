package org.testin.util.indexer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.*;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

final class IndexingScanner {

    private final Project project;
    private final IndexerDataStore store;

    IndexingScanner(final @NotNull Project project, final @NotNull IndexerDataStore store) {
        this.project = project;
        this.store = store;
    }

    void scanProject(final Path projectPath, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final Mapper mapper = Services.getInstance(project, Mapper.class);
            final DirectoryMapper dirMapper = Services.getInstance(project, DirectoryMapper.class);

            final TestProjectMarker marker = mapper.readValue(
                    projectPath.resolve(DirectoryType.TP.getMarker()).toFile(),
                    TestProjectMarker.class);
            if (marker == null) return;

            final String fileName = projectPath.getFileName().toString();
            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(projectPath)
                    .pathName(fileName)
                    .path2(tools.buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            store.getTestProjectsByPath().put(projectPath.toString(), tp);

            indicator.setFraction(0.1);
            indicator.setText(fileName + " - test sets...");

            final Path tcDir = projectPath.resolve(DirectoryType.TCD.getDisplayedName());
            if (Files.exists(tcDir) && Files.isDirectory(tcDir)) {
                final TestCasesMainDirectoryDto tcd = dirMapper.readTestCasesRootNode(project, tcDir, tp);
                if (tcd != null) {
                    tp.setTestCasesDirectory(tcd);
                    store.getTestCasesMainDirsByPath().put(tcDir.toString(), tcd);
                    scanTestSets(tcDir, tcd, indicator);
                }
            }

            indicator.setFraction(0.5);
            indicator.setText(fileName + " - test runs...");

            final Path trDir = projectPath.resolve(DirectoryType.TRD.getDisplayedName());
            if (Files.exists(trDir) && Files.isDirectory(trDir)) {
                final TestRunsMainDirectoryDto trd = dirMapper.readTestRunsRootNode(project, trDir, tp);
                if (trd != null) {
                    tp.setTestRunsDirectory(trd);
                    store.getTestRunsMainDirsByPath().put(trDir.toString(), trd);
                    scanTestRunDirs(trDir, trd, indicator);
                }
            }

            indicator.setFraction(1.0);
            indicator.setText(fileName + " - done.");

        } catch (Exception e) {
            Log.error("Failed to scan project: " + projectPath.getFileName() + " - " + e.getMessage());
        }
    }

    private void scanTestSets(final Path tcDir, final DirectoryDto parent,
                              final ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final List<Path> dirs = paths.filter(Files::isDirectory).toList();

            dirs.parallelStream().forEach(dirPath -> {
                if (Files.exists(dirPath.resolve(DirectoryType.TS.getMarker()))) {
                    scanTestSet(dirPath, parent, indicator);
                } else if (Files.exists(dirPath.resolve(DirectoryType.TSP.getMarker()))) {
                    scanTestSetPackage(dirPath, parent, indicator);
                }
            });
        } catch (Exception e) {
            Log.error("Failed to list test sets: " + e.getMessage());
        }
    }

    private void scanTestSetPackage(final Path path, final DirectoryDto parent,
                                    final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(project, DirectoryMapper.class);
            final TestSetPackageDirectoryDto tsp = dirMapper.readTestSetPackageNode(project, path, parent);
            if (tsp == null) return;

            store.getTestSetPackagesByPath().put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .parallel()
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TS.getMarker()))) {
                                scanTestSet(subPath, tsp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TSP.getMarker()))) {
                                scanTestSetPackage(subPath, tsp, indicator);
                            }
                        });
            }

        } catch (Exception e) {
            Log.error("Failed to scan test set package: " + path.getFileName());
        }
    }

    private void scanTestSet(final Path path, final DirectoryDto parent,
                             final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(project, DirectoryMapper.class);
            final TestSetDirectoryDto ts = dirMapper.readTestSetNode(project, path, parent);
            if (ts == null) return;

            store.getTestSetsByPath().put(path.toString(), ts);

            final List<UUID> caseIds = Collections.synchronizedList(new ArrayList<>());
            final Mapper mapper = Services.getInstance(project, Mapper.class);

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = mapper.readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
                                    tc.setParent(ts);
                                    store.getTestCasesById().put(tc.getId(), tc);
                                    caseIds.add(tc.getId());
                                }
                            } catch (Exception ex) {
                                Log.error("Failed to read test case '" + filePath.toAbsolutePath() +
                                        "': " + ex.getMessage());
                            }
                        });
            }

            store.getTestSetCaseIds().put(path.toString(), caseIds);
            indicator.setText("Test set: " + ts.getName() + " (" + caseIds.size() + " cases)");

        } catch (Exception e) {
            Log.error("Failed to scan test set '" +
                    (path != null ? path.getFileName().toString() : "null") + "': " + e.getMessage());
        }
    }

    private void scanTestRunDirs(final Path trDir, final DirectoryDto parent,
                                 final ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final List<Path> dirs = paths.filter(Files::isDirectory).toList();

            dirs.parallelStream().forEach(dirPath -> {
                if (Files.exists(dirPath.resolve(DirectoryType.TR.getMarker()))) {
                    scanTestRun(dirPath, parent, indicator);
                } else if (Files.exists(dirPath.resolve(DirectoryType.TRP.getMarker()))) {
                    scanTestRunPackageDir(dirPath, parent, indicator);
                }
            });
        } catch (Exception e) {
            Log.error("Failed to list test runs: " + e.getMessage());
        }
    }

    private void scanTestRunPackageDir(final Path path, final DirectoryDto parent,
                                       final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(project, DirectoryMapper.class);
            final TestRunPackageDirectoryDto trp = dirMapper.readTestRunPackageNode(project, path, parent);
            if (trp == null) return;

            store.getTestRunPackagesByPath().put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .parallel()
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TR.getMarker()))) {
                                scanTestRun(subPath, trp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TRP.getMarker()))) {
                                scanTestRunPackageDir(subPath, trp, indicator);
                            }
                        });
            }

        } catch (Exception e) {
            Log.error("Failed to scan test run package: " + path.getFileName());
        }
    }

    private void scanTestRun(final Path path, final DirectoryDto parent,
                             final ProgressIndicator indicator) {
        try {
            final DirectoryMapper dirMapper = Services.getInstance(project, DirectoryMapper.class);
            final TestRunDirectoryDto tr = dirMapper.readTestRunNode(project, path, parent);
            if (tr == null) return;

            store.getTestRunDirsByPath().put(path.toString(), tr);

            final String fileName = path.getFileName().toString();
            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final Mapper mapper = Services.getInstance(project, Mapper.class);
                final TestRunDto trr = mapper.readValue(jsonPath.toFile(), TestRunDto.class);
                if (trr != null) {
                    store.getTestRunsByPath().put(path.toString(), trr);
                }
            }

            indicator.setText("Test run: " + fileName);

        } catch (Exception e) {
            Log.error("Failed to scan test run '" +
                    (path != null ? path.getFileName().toString() : "null") + "': " + e.getMessage());
        }
    }
}
