package org.testin.util.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.TestRunMarker;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.pojo.dto.dirs.*;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.settings.Setting;
import org.testin.util.FilesUtil;
import org.testin.util.Mapper;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {

    private final Project project;
    private final AtomicBoolean indexed = new AtomicBoolean(false);
    private final AtomicBoolean indexing = new AtomicBoolean(false);

    @Getter
    private final Map<UUID, TestCaseDto> testCasesById = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, TestRunDto> testRunsById = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestProjectDirectoryDto> testProjectsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestSetDirectoryDto> testSetsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunDirectoryDto> testRunDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestSetPackageDirectoryDto> testSetPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunPackageDirectoryDto> testRunPackagesByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestCasesMainDirectoryDto> testCasesMainDirsByPath = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, TestRunsMainDirectoryDto> testRunsMainDirsByPath = new ConcurrentHashMap<>();

    private final Map<String, List<UUID>> testSetCaseIds = new ConcurrentHashMap<>();

    private final Map<String, TestRunDto> testRunsByPath = new ConcurrentHashMap<>();

    public ProjectIndexer(final @NotNull Project project) {
        this.project = project;
    }

    public void indexWithProgress() {
        if (indexed.get() || indexing.getAndSet(true)) return;

        final Path rootPath = Services.getInstance(project, Setting.class).getTestinPath();
        if (rootPath.toString().isEmpty()) {
            indexing.set(false);
            return;
        }

        final Path absoluteRoot = rootPath.isAbsolute() ? rootPath
                : (project.getBasePath() != null
                   ? Path.of(project.getBasePath(), rootPath.toString())
                   : rootPath);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Indexing Testin data...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Scanning test projects...");
                indicator.setFraction(0.0);

                try {
                    indexAll(absoluteRoot, indicator);
                } catch (Exception e) {
                    Log.error("Indexing failed: " + e.getMessage());
                }

                indexed.set(true);
                indexing.set(false);

                Log.info("ProjectIndexer: indexing complete. " +
                        testCasesById.size() + " test cases, " +
                        testRunsById.size() + " test runs, " +
                        testProjectsByPath.size() + " projects indexed.");
            }

            @Override
            public void onFinished() {
                ApplicationManager.getApplication().invokeLater(() -> Log.info("ProjectIndexer: indexing finished, notifying listeners."));
            }
        });
    }

    public void awaitIndexing() {
        while (!indexed.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public boolean isIndexed() {
        return indexed.get();
    }

    private void indexAll(final Path rootPath, final ProgressIndicator indicator) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return;

        final Path[] projectPaths;
        try (Stream<Path> dirs = Files.list(rootPath)) {
            projectPaths = dirs.filter(Files::isDirectory).toArray(Path[]::new);
        } catch (Exception e) {
            Log.error("Failed to list root directory: " + e.getMessage());
            return;
        }

        if (projectPaths.length == 0) return;

        final double projectWeight = 1.0 / projectPaths.length;

        for (int i = 0; i < projectPaths.length; i++) {
            final Path projectPath = projectPaths[i];
            if (!Files.exists(projectPath.resolve(DirectoryType.TP.getMarker()))) continue;

            indicator.setText("Indexing project: " + projectPath.getFileName());
            indicator.setFraction(i * projectWeight);

            indexProject(projectPath, indicator);

            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        indicator.setFraction(1.0);
        indicator.setText("Indexing complete.");
    }

    private void indexProject(final Path projectPath, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final TestProjectMarker marker = Services.getInstance(project, Mapper.class)
                    .readValue(projectPath.resolve(DirectoryType.TP.getMarker()).toFile(), TestProjectMarker.class);
            if (marker == null) return;

            final String fileName = projectPath.getFileName().toString();
            final TestProjectDirectoryDto tp = TestProjectDirectoryDto.builder()
                    .name(fileName)
                    .path(projectPath)
                    .pathName(fileName)
                    .fqcn(List.of(tools.sanitizePackageName(fileName)))
                    .path2(tools.buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            testProjectsByPath.put(projectPath.toString(), tp);

            final Path tcDir = projectPath.resolve(DirectoryType.TCD.getDisplayedName());
            if (Files.exists(tcDir) && Files.isDirectory(tcDir)) {
                final TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto.builder()
                        .path(tcDir)
                        .name(DirectoryType.TCD.getDisplayedName())
                        .parent(tp)
                        .fqcn(tp.getFqcn())
                        .path2(tools.buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                        .build();
                tp.setTestCasesDirectory(tcd);
                testCasesMainDirsByPath.put(tcDir.toString(), tcd);
                indexTestSets(tcDir, tcd, indicator);
            }

            final Path trDir = projectPath.resolve(DirectoryType.TRD.getDisplayedName());
            if (Files.exists(trDir) && Files.isDirectory(trDir)) {
                final TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto.builder()
                        .path(trDir)
                        .name(DirectoryType.TRD.getDisplayedName())
                        .parent(tp)
                        .fqcn(tp.getFqcn())
                        .path2(tools.buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                        .build();
                tp.setTestRunsDirectory(trd);
                testRunsMainDirsByPath.put(trDir.toString(), trd);
                indexTestRunDirs(trDir, trd, indicator);
            }

        } catch (Exception e) {
            Log.error("Failed to index project: " + projectPath.getFileName() + " - " + e.getMessage());
        }
    }

    private void indexTestSets(final Path tcDir, final DirectoryDto parent, final ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            paths.filter(Files::isDirectory)
                    .forEach(dirPath -> {
                        if (Files.exists(dirPath.resolve(DirectoryType.TS.getMarker()))) {
                            indexTestSet(dirPath, parent, indicator);
                        } else if (Files.exists(dirPath.resolve(DirectoryType.TSP.getMarker()))) {
                            indexTestSetPackage(dirPath, parent, indicator);
                        }
                    });
        } catch (Exception e) {
            Log.error("Failed to list test sets: " + e.getMessage());
        }
    }

    private void indexTestSetPackage(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final String fileName = path.getFileName().toString();
            final TestSetPackageDirectoryDto tsp = TestSetPackageDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(tools.appendFqcn(parent.getFqcn(), fileName, DirectoryType.TSP))
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .build();

            testSetPackagesByPath.put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TS.getMarker()))) {
                                indexTestSet(subPath, tsp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TSP.getMarker()))) {
                                indexTestSetPackage(subPath, tsp, indicator);
                            }
                        });
            }

        } catch (Exception e) {
            Log.error("Failed to index test set package: " + path.getFileName());
        }
    }

    private void indexTestSet(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final String fileName = path.getFileName().toString();
            final TestSetDirectoryDto ts = TestSetDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(tools.appendFqcn(parent.getFqcn(), fileName, DirectoryType.TS))
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .build();

            testSetsByPath.put(path.toString(), ts);

            final List<UUID> caseIds = new ArrayList<>();
            final List<String> baseFqcn = ts.getFqcn();
            final List<String> basePath2 = ts.getPath2();

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = Services.getInstance(project, Mapper.class)
                                        .readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
                                    tc.setPath(new ArrayList<>(basePath2));
                                    List<String> tcFqcn = new ArrayList<>(baseFqcn);
                                    tcFqcn.add(tools.sanitizeMethodName(tc.getDescription()));
                                    tc.setFqcn(tcFqcn);

                                    testCasesById.put(tc.getId(), tc);
                                    caseIds.add(tc.getId());
                                }
                            } catch (Exception ex) {
                                Log.error("Failed to read test case: " + filePath.getFileName());
                            }
                        });
            }

            testSetCaseIds.put(path.toString(), caseIds);

            indicator.setText("Indexing test set: " + fileName + " (" + caseIds.size() + " cases)");

        } catch (Exception e) {
            Log.error("Failed to index test set: " + path.getFileName());
        }
    }

    private void indexTestRunDirs(final Path trDir, final DirectoryDto parent, final ProgressIndicator indicator) {
        try (Stream<Path> paths = Files.list(trDir)) {
            paths.filter(Files::isDirectory)
                    .forEach(dirPath -> {
                        if (Files.exists(dirPath.resolve(DirectoryType.TR.getMarker()))) {
                            indexTestRunDir(dirPath, parent, indicator);
                        } else if (Files.exists(dirPath.resolve(DirectoryType.TRP.getMarker()))) {
                            indexTestRunPackageDir(dirPath, parent, indicator);
                        }
                    });
        } catch (Exception e) {
            Log.error("Failed to list test runs: " + e.getMessage());
        }
    }

    private void indexTestRunPackageDir(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final String fileName = path.getFileName().toString();
            final TestRunPackageDirectoryDto trp = TestRunPackageDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(tools.appendFqcn(parent.getFqcn(), fileName, DirectoryType.TRP))
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .build();

            testRunPackagesByPath.put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TR.getMarker()))) {
                                indexTestRunDir(subPath, trp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TRP.getMarker()))) {
                                indexTestRunPackageDir(subPath, trp, indicator);
                            }
                        });
            }

        } catch (Exception e) {
            Log.error("Failed to index test run package: " + path.getFileName());
        }
    }

    private void indexTestRunDir(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final String fileName = path.getFileName().toString();

            final TestRunMarker marker = Services.getInstance(project, Mapper.class)
                    .readValue(path.resolve(DirectoryType.TR.getMarker()).toFile(), TestRunMarker.class);

            final TestRunDirectoryDto trd = TestRunDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .fqcn(tools.appendFqcn(parent.getFqcn(), fileName, DirectoryType.TR))
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            testRunDirsByPath.put(path.toString(), trd);

            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final TestRunDto tr = Services.getInstance(project, Mapper.class)
                        .readValue(jsonPath.toFile(), TestRunDto.class);
                if (tr != null) {
                    testRunsByPath.put(path.toString(), tr);
                }
            }

            indicator.setText("Indexing test run: " + fileName);

        } catch (Exception e) {
            Log.error("Failed to index test run: " + path.getFileName());
        }
    }

    public List<TestCaseDto> getTestCasesForTestSet(final Path testSetPath) {
        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        final List<TestCaseDto> result = new ArrayList<>(ids.size());
        for (final UUID id : ids) {
            final TestCaseDto tc = testCasesById.get(id);
            if (tc != null) result.add(tc);
        }
        return result;
    }

    public TestRunDto getTestRunForPath(final Path testRunPath) {
        return testRunsByPath.get(testRunPath.toString());
    }

    public TestCaseDto getTestCaseById(final UUID id) {
        return testCasesById.get(id);
    }

    public void putTestCase(final Path testSetPath, final TestCaseDto tc) {
        testCasesById.put(tc.getId(), tc);

        final List<UUID> ids = testSetCaseIds.computeIfAbsent(testSetPath.toString(), k -> Collections.synchronizedList(new ArrayList<>()));
        if (!ids.contains(tc.getId())) {
            ids.add(tc.getId());
        }

        Services.getInstance(project, org.testin.util.FilesUtil.class).write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
    }

    public void removeTestCase(final Path testSetPath, final UUID tcId) {
        testCasesById.remove(tcId);

        final List<UUID> ids = testSetCaseIds.get(testSetPath.toString());
        if (ids != null) ids.remove(tcId);

        final Path filePath = testSetPath.resolve(tcId + ".json");
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            Log.error("Failed to delete test case file: " + filePath);
        }
    }

    public void updateSequence(final Path testSetPath, final List<TestCaseDto> sortedList) {
        final String pathStr = testSetPath.toString();
        final List<UUID> ids = new ArrayList<>(sortedList.size());

        final Set<UUID> newIds = new HashSet<>();

        for (int i = 0; i < sortedList.size(); i++) {
            final TestCaseDto tc = sortedList.get(i);
            tc.setIsHead(i == 0);
            tc.setNext(i < sortedList.size() - 1 ? sortedList.get(i + 1).getId() : null);
            ids.add(tc.getId());
            newIds.add(tc.getId());
            testCasesById.put(tc.getId(), tc);

            Services.getInstance(project, org.testin.util.FilesUtil.class).write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
        }

        final List<UUID> oldIds = testSetCaseIds.get(pathStr);
        if (oldIds != null) {
            for (final UUID oldId : oldIds) {
                if (!newIds.contains(oldId)) {
                    testCasesById.remove(oldId);
                }
            }
        }

        testSetCaseIds.put(pathStr, ids);
    }

    public void putTestRun(final Path testRunPath, final TestRunDto tr) {
        testRunsByPath.put(testRunPath.toString(), tr);

        Services.getInstance(project, FilesUtil.class).write(project, testRunPath.resolve(testRunPath.getFileName() + ".json"), tr);
    }

    public List<DirectoryDto> getChildren(final Path parentPath) {
        final String parentStr = parentPath.toString();
        final List<DirectoryDto> children = new ArrayList<>();

        for (final TestSetPackageDirectoryDto dto : testSetPackagesByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }

        for (final TestSetDirectoryDto dto : testSetsByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }

        for (final TestRunPackageDirectoryDto dto : testRunPackagesByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }

        for (final TestRunDirectoryDto dto : testRunDirsByPath.values()) {
            if (dto.getParent() != null && dto.getParent().getPath().toString().equals(parentStr)) {
                children.add(dto);
            }
        }

        children.sort(Comparator.comparing(DirectoryDto::getName));
        return children;
    }

    public void dispose() {
        testCasesById.clear();
        testRunsById.clear();
        testProjectsByPath.clear();
        testSetsByPath.clear();
        testRunDirsByPath.clear();
        testSetPackagesByPath.clear();
        testRunPackagesByPath.clear();
        testCasesMainDirsByPath.clear();
        testRunsMainDirsByPath.clear();
        testSetCaseIds.clear();
        testRunsByPath.clear();
        indexed.set(false);
    }
}
