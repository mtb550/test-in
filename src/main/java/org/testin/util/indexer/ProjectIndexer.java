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
import org.testin.util.MemoryEstimator;
import org.testin.util.Tools;
import org.testin.util.logger.Log;
import org.testin.util.services.EditorStateService;
import org.testin.util.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service(Service.Level.PROJECT)
public final class ProjectIndexer {

    private final Project project;

    private final AtomicBoolean indexed = new AtomicBoolean(false);

    private final AtomicBoolean indexing = new AtomicBoolean(false);

    private final AtomicBoolean restoreEditorsOnComplete = new AtomicBoolean(true);

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

    private volatile CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project project) {
        this.project = project;
    }

    public void indexWithProgress() {
        try {
            if (indexed.get() || indexing.getAndSet(true)) {
                return;
            }

            final Path rootPath = Services.getInstance(project, Setting.class).getTestinPath();
            if (rootPath.toString().isEmpty()) {
                indexing.set(false);
                return;
            }

            final Path absoluteRoot = rootPath.isAbsolute() ? rootPath
                    : (project.getBasePath() != null
                       ? Path.of(project.getBasePath(), rootPath.toString())
                       : rootPath);

            final List<Path> validProjects = collectValidProjects(absoluteRoot);
            if (validProjects.isEmpty()) {
                indexing.set(false);
                Log.warn("indexWithProgress: No valid projects found at '" + absoluteRoot.toAbsolutePath() + "'");
                return;
            }

            indexingLatch = new CountDownLatch(validProjects.size());

            Log.info("Indexing " + validProjects.size() + " projects with per-project progress...");

            for (final Path projectPath : validProjects) {
                final String projectName = projectPath.getFileName().toString();

                ProgressManager.getInstance()
                        .run(new Task.Backgroundable(project, "Testin indexing - " + projectName, true) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                indicator.setIndeterminate(false);
                                indicator.setFraction(0.0);
                                indicator.setText("Indexing " + projectName + "...");

                                try {
                                    indexProject(projectPath, indicator);
                                } catch (Exception e) {
                                    Log.error(ProjectIndexer.class.getSimpleName(), "Failed to index project: " + projectName + " - " + e.getMessage());
                                }

                                indicator.setFraction(1.0);
                                indicator.setText("Done - " + projectName);
                            }

                            @Override
                            public void onSuccess() {
                                indexingLatch.countDown();
                                Log.info(ProjectIndexer.class.getSimpleName(), "Project '" + projectName + "' indexed successfully.");

                                if (indexingLatch.getCount() == 0 && indexed.compareAndSet(false, true)) {
                                    indexing.set(false);

                                    try {
                                        final long estimatedBytes = MemoryEstimator.estimate(
                                                testCasesById, testRunsById,
                                                testProjectsByPath, testSetsByPath,
                                                testRunDirsByPath, testSetPackagesByPath,
                                                testRunPackagesByPath, testCasesMainDirsByPath,
                                                testRunsMainDirsByPath, testSetCaseIds,
                                                testRunsByPath);

                                        MemoryEstimator.logStats(
                                                "Indexer Memory Stats",
                                                testCasesById.size(), testRunsById.size(),
                                                testProjectsByPath.size(), testSetsByPath.size(),
                                                testRunDirsByPath.size(), testSetPackagesByPath.size(),
                                                testRunPackagesByPath.size(), testCasesMainDirsByPath.size(),
                                                testRunsMainDirsByPath.size(),
                                                testSetCaseIds.size(),
                                                testSetCaseIds.values().stream().mapToInt(List::size).sum(),
                                                testRunsByPath.size(),
                                                estimatedBytes);

                                        Log.info(ProjectIndexer.class.getSimpleName(), "indexing complete. " +
                                                testCasesById.size() + " test cases, " +
                                                testRunsById.size() + " test runs, " +
                                                testProjectsByPath.size() + " projects indexed.");

                                    } catch (Exception e) {
                                        Log.error(ProjectIndexer.class.getSimpleName(), "Error logging indexer stats: " + e.getMessage());
                                    }

                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        if (restoreEditorsOnComplete.compareAndSet(true, true)) {
                                            Log.info(ProjectIndexer.class.getSimpleName(), "indexing finished, restoring open editors.");
                                            Services.getInstance(project, EditorStateService.class).restoreOpenEditors();
                                        } else {
                                            Log.info(ProjectIndexer.class.getSimpleName(), "indexing finished, skipping editor restore.");
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onThrowable(@NotNull Throwable error) {
                                indexingLatch.countDown();
                                Log.error(ProjectIndexer.class.getSimpleName(),
                                        "ProjectIndexer encountered an error indexing '" + projectName + "': " + error.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            Log.error(ProjectIndexer.class.getSimpleName(), "indexWithProgress: Failed to index projects: " + e.getMessage());
            indexing.set(false);
        }
    }

    public void awaitIndexing() {
        if (indexed.get()) return;
        try {
            indexingLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isIndexed() {
        return indexed.get();
    }

    private List<Path> collectValidProjects(final Path rootPath) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return Collections.emptyList();

        final Path[] projectPaths;
        try (Stream<Path> dirs = Files.list(rootPath)) {
            projectPaths = dirs.filter(Files::isDirectory).toArray(Path[]::new);
        } catch (Exception e) {
            Log.error("Failed to list root directory: " + e.getMessage());
            return Collections.emptyList();
        }

        if (projectPaths.length == 0) return Collections.emptyList();

        return Arrays.stream(projectPaths)
                .filter(p -> Files.exists(p.resolve(DirectoryType.TP.getMarker())))
                .collect(Collectors.toList());
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
                    .path2(tools.buildPath2(null, fileName))
                    .marker(marker)
                    .build();

            testProjectsByPath.put(projectPath.toString(), tp);

            indicator.setFraction(0.1);
            indicator.setText(fileName + " - test sets...");

            final Path tcDir = projectPath.resolve(DirectoryType.TCD.getDisplayedName());
            if (Files.exists(tcDir) && Files.isDirectory(tcDir)) {
                final TestCasesMainDirectoryDto tcd = TestCasesMainDirectoryDto.builder()
                        .path(tcDir)
                        .name(DirectoryType.TCD.getDisplayedName())
                        .parent(tp)
                        .path2(tools.buildPath2(tp.getPath2(), DirectoryType.TCD.getDisplayedName()))
                        .build();

                tp.setTestCasesDirectory(tcd);
                testCasesMainDirsByPath.put(tcDir.toString(), tcd);
                indexTestSets(tcDir, tcd, indicator);
            }

            indicator.setFraction(0.5);
            indicator.setText(fileName + " - test runs...");

            final Path trDir = projectPath.resolve(DirectoryType.TRD.getDisplayedName());
            if (Files.exists(trDir) && Files.isDirectory(trDir)) {
                final TestRunsMainDirectoryDto trd = TestRunsMainDirectoryDto.builder()
                        .path(trDir)
                        .name(DirectoryType.TRD.getDisplayedName())
                        .parent(tp)
                        .path2(tools.buildPath2(tp.getPath2(), DirectoryType.TRD.getDisplayedName()))
                        .build();

                tp.setTestRunsDirectory(trd);
                testRunsMainDirsByPath.put(trDir.toString(), trd);
                indexTestRunDirs(trDir, trd, indicator);
            }

            indicator.setFraction(1.0);
            indicator.setText(fileName + " - done.");
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
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .build();

            testSetsByPath.put(path.toString(), ts);

            final List<UUID> caseIds = Collections.synchronizedList(new ArrayList<>());

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .parallel()
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
                                    tc.setParent(ts);
                                    testCasesById.put(tc.getId(), tc);
                                    caseIds.add(tc.getId());

                                } else {
                                    Log.error("indexTestSet: Mapper returned null for file: " + filePath.toAbsolutePath());
                                }

                            } catch (Exception ex) {
                                Log.error("indexTestSet: Failed to read test case file '" + filePath.toAbsolutePath() + "': " + ex.getMessage());
                            }
                        });
            }

            testSetCaseIds.put(path.toString(), caseIds);

            indicator.setText("Test set: " + fileName + " (" + caseIds.size() + " cases)");

        } catch (Exception e) {
            Log.error("Failed to index test set '" + (path != null ? path.getFileName().toString() : "null") + "': " + e.getMessage());
        }
    }

    private void indexTestRunDirs(final Path trDir, final DirectoryDto parent, final ProgressIndicator indicator) {
        Log.info("indexTestRunDirs: Scanning '" + trDir.toAbsolutePath() + "' for test run directories...");
        try (Stream<Path> paths = Files.list(trDir)) {
            final List<Path> dirs = paths.filter(Files::isDirectory).toList();
            Log.info("indexTestRunDirs: Found " + dirs.size() + " subdirectories in " + trDir.getFileName());

            for (final Path dirPath : dirs) {
                final Path trMarker = dirPath.resolve(DirectoryType.TR.getMarker());
                final Path trpMarker = dirPath.resolve(DirectoryType.TRP.getMarker());
                Log.info("indexTestRunDirs:   Checking '" + dirPath.getFileName() + "' - .tr exists: " + Files.exists(trMarker) + ", .trp exists: " + Files.exists(trpMarker));

                if (Files.exists(trMarker)) {
                    indexTestRun(dirPath, parent, indicator);

                } else if (Files.exists(trpMarker)) {
                    indexTestRunPackageDir(dirPath, parent, indicator);
                }
            }
        } catch (Exception e) {
            Log.error("indexTestRunDirs: Failed to list test runs in '" + trDir.toAbsolutePath() + "': " + e.getMessage());
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
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .build();

            testRunPackagesByPath.put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            if (Files.exists(subPath.resolve(DirectoryType.TR.getMarker()))) {
                                indexTestRun(subPath, trp, indicator);
                            } else if (Files.exists(subPath.resolve(DirectoryType.TRP.getMarker()))) {
                                indexTestRunPackageDir(subPath, trp, indicator);
                            }
                        });
            }

        } catch (Exception e) {
            Log.error("Failed to index test run package: " + path.getFileName());
        }
    }

    private void indexTestRun(final Path path, final DirectoryDto parent, final ProgressIndicator indicator) {
        try {
            final Tools tools = Services.getInstance(project, Tools.class);
            final String fileName = path.getFileName().toString();

            final Path markerPath = path.resolve(DirectoryType.TR.getMarker());
            final TestRunMarker marker = Services.getInstance(project, Mapper.class).readValue(markerPath.toFile(), TestRunMarker.class);

            if (marker == null) {
                Log.error("indexTestRun: Failed to parse .tr marker at '" + markerPath.toAbsolutePath() + "' - returned null");
                return;
            }

            final TestRunDirectoryDto tr = TestRunDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            testRunDirsByPath.put(path.toString(), tr);
            Log.info("indexTestRun: Indexed test run directory '" + fileName + "' at " + path.toAbsolutePath());

            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                Log.info("indexTestRun: Reading test run data from " + jsonPath.toAbsolutePath());
                final TestRunDto trr = Services.getInstance(project, Mapper.class)
                        .readValue(jsonPath.toFile(), TestRunDto.class);

                if (trr != null) {
                    testRunsByPath.put(path.toString(), trr);
                    Log.info("indexTestRun: Loaded test run data with " +
                            trr.getResults().size() + " results");
                } else {
                    Log.error("indexTestRun: Mapper returned null for test run file '" + jsonPath.toAbsolutePath() + "'");
                }

            } else {
                Log.warn("indexTestRun: No JSON file found at " + jsonPath.toAbsolutePath());
            }

            indicator.setText("Test run: " + fileName);

        } catch (Exception e) {
            Log.error("indexTestRun: Failed to index test run '" + (path != null ? path.getFileName().toString() : "null") + "': " + e.getMessage());
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

        Services.getInstance(project, FilesUtil.class).write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
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

            Services.getInstance(project, FilesUtil.class).write(project, testSetPath.resolve(tc.getId() + ".json"), tc);
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

    public void addTestSet(final TestSetDirectoryDto ts) {
        testSetsByPath.put(ts.getPath().toString(), ts);
    }

    public void addTestSetPackage(final TestSetPackageDirectoryDto tsp) {
        testSetPackagesByPath.put(tsp.getPath().toString(), tsp);
    }

    public void addTestRunDir(final TestRunDirectoryDto trd) {
        testRunDirsByPath.put(trd.getPath().toString(), trd);
    }

    public void addTestRunPackage(final TestRunPackageDirectoryDto trp) {
        testRunPackagesByPath.put(trp.getPath().toString(), trp);
    }

    public void updateProjectMarker(final Project project, final Path projectPath, final TestProjectMarker marker) {
        final TestProjectDirectoryDto tp = testProjectsByPath.get(projectPath.toString());
        if (tp != null) {
            tp.setMarker(marker);
        }
        Services.getInstance(project, FilesUtil.class).write(project, projectPath.resolve(DirectoryType.TP.getMarker()), marker);
    }

    public void updateRunMarker(final Project project, final Path runPath, final TestRunMarker marker) {
        final TestRunDirectoryDto trd = testRunDirsByPath.get(runPath.toString());
        if (trd != null) {
            trd.setMarker(marker);
        }
        Services.getInstance(project, FilesUtil.class).write(project, runPath.resolve(DirectoryType.TR.getMarker()), marker);
    }

    public TestRunDirectoryDto getTestRunDirByPath(final Path path) {
        return testRunDirsByPath.get(path.toString());
    }

    public TestSetDirectoryDto getTestSetByPath(final Path path) {
        return testSetsByPath.get(path.toString());
    }

    public void renameNode(final Path oldPath, final Path newPath) {
        final String oldStr = oldPath.toString();
        final String newStr = newPath.toString();

        renameMapEntry(testProjectsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunDirsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetPackagesByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunPackagesByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testCasesMainDirsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testRunsMainDirsByPath, oldStr, newStr, dto -> dto.setPath(newPath));
        renameMapEntry(testSetCaseIds, oldStr, newStr, ids -> {
        });
        renameMapEntry(testRunsByPath, oldStr, newStr, tr -> {
        });

        updatePath2IfNeeded(testProjectsByPath.get(newStr), newPath);
        updatePath2IfNeeded(testSetsByPath.get(newStr), newPath);
        updatePath2IfNeeded(testRunDirsByPath.get(newStr), newPath);
        updatePath2IfNeeded(testSetPackagesByPath.get(newStr), newPath);
        updatePath2IfNeeded(testRunPackagesByPath.get(newStr), newPath);
    }

    private void updatePath2IfNeeded(final DirectoryDto dto, final Path newPath) {
        if (dto != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            dto.setPath2(tools.buildPath2(dto.getParent() != null ? dto.getParent().getPath2() : null, newName));
        }
    }

    private <V> void renameMapEntry(final Map<String, V> map, final String oldKey, final String newKey, final java.util.function.Consumer<V> updater) {
        final V value = map.remove(oldKey);
        if (value != null) {
            updater.accept(value);
            map.put(newKey, value);
        }
    }

    public void addTestProject(final TestProjectDirectoryDto tp) {
        testProjectsByPath.put(tp.getPath().toString(), tp);
        if (tp.getTestCasesDirectory() != null) {
            testCasesMainDirsByPath.put(tp.getTestCasesDirectory().getPath().toString(), tp.getTestCasesDirectory());
        }
        if (tp.getTestRunsDirectory() != null) {
            testRunsMainDirsByPath.put(tp.getTestRunsDirectory().getPath().toString(), tp.getTestRunsDirectory());
        }
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
        clearAll();
        Log.info("Indexer disposed");
    }

    public void resetForReindex() {
        restoreEditorsOnComplete.set(false);
        clearAll();
        Log.info("Indexer reset for re-indexing");
    }

    private void clearAll() {
        final long actualBytes = MemoryEstimator.measureActual(() -> {
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
        });

        Log.info("Indexer cleared — RAM freed: " + MemoryEstimator.formatBytes(actualBytes));

        indexed.set(false);
        indexing.set(false);
        indexingLatch = new CountDownLatch(1);
    }
}
