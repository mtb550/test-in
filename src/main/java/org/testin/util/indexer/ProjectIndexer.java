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
import java.util.concurrent.CountDownLatch;
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

    private volatile CountDownLatch indexingLatch = new CountDownLatch(1);

    public ProjectIndexer(final @NotNull Project project) {
        this.project = project;
    }

    public void indexWithProgress() {
        if (indexed.get() || indexing.getAndSet(true)) {
            return;
        }

        indexingLatch = new CountDownLatch(1);

        final Path rootPath = Services.getInstance(project, Setting.class).getTestinPath();
        if (rootPath.toString().isEmpty()) {
            indexing.set(false);
            indexingLatch.countDown();
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
                indexingLatch.countDown();

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

            final List<UUID> caseIds = new ArrayList<>();

            try (Stream<Path> files = Files.list(path)) {
                files.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(filePath -> {
                            try {
                                final TestCaseDto tc = Services.getInstance(project, Mapper.class).readValue(filePath.toFile(), TestCaseDto.class);
                                if (tc != null) {
                                    tc.setParent(ts);
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
                            indexTestRun(dirPath, parent, indicator);
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

            final TestRunMarker marker = Services.getInstance(project, Mapper.class)
                    .readValue(path.resolve(DirectoryType.TR.getMarker()).toFile(), TestRunMarker.class);

            final TestRunDirectoryDto tr = TestRunDirectoryDto.builder()
                    .name(fileName)
                    .path(path)
                    .parent(parent)
                    .path2(tools.buildPath2(parent.getPath2(), fileName))
                    .marker(marker)
                    .build();

            testRunDirsByPath.put(path.toString(), tr);

            final Path jsonPath = path.resolve(fileName + ".json");
            if (Files.exists(jsonPath)) {
                final TestRunDto trr = Services.getInstance(project, Mapper.class)
                        .readValue(jsonPath.toFile(), TestRunDto.class);
                if (trr != null) {
                    testRunsByPath.put(path.toString(), trr);
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

    /**
     * Rename a node in the index: updates all maps and DTO paths.
     * Handles all node types (project, test set, test run, packages).
     * The parent references of children are automatically correct because
     * the parent DTO objects are shared (updated in-place by renameMapEntry).
     */
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

        final TestProjectDirectoryDto tp = testProjectsByPath.get(newStr);
        if (tp != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            tp.setPath2(tools.buildPath2(tp.getParent() != null ? tp.getParent().getPath2() : null, newName));
        }

        final TestSetDirectoryDto ts = testSetsByPath.get(newStr);
        if (ts != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            ts.setPath2(tools.buildPath2(ts.getParent() != null ? ts.getParent().getPath2() : null, newName));
        }

        final TestRunDirectoryDto trd = testRunDirsByPath.get(newStr);
        if (trd != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            trd.setPath2(tools.buildPath2(trd.getParent() != null ? trd.getParent().getPath2() : null, newName));
        }

        final TestSetPackageDirectoryDto tsp = testSetPackagesByPath.get(newStr);
        if (tsp != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            tsp.setPath2(tools.buildPath2(tsp.getParent() != null ? tsp.getParent().getPath2() : null, newName));
        }

        final TestRunPackageDirectoryDto trp = testRunPackagesByPath.get(newStr);
        if (trp != null && newPath.getFileName() != null) {
            final String newName = newPath.getFileName().toString();
            final Tools tools = Services.getInstance(project, Tools.class);
            trp.setPath2(tools.buildPath2(trp.getParent() != null ? trp.getParent().getPath2() : null, newName));
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
        indexingLatch = new CountDownLatch(1);
    }
}
