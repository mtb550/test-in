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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
            final @NotNull TestProjectDirectoryDto tp = Services.getInstance(p, DirectoryMapper.class).getTestProjectNode(p, projectPath);

            if (tp.getMarker().getStatus() == ProjectStatus.ARCHIVED) {
                Logger.info("Skipping archived project: " + projectPath.getFileName());
                return;
            }

            store.getTestProjectsByPath().put(projectPath.toString(), tp);

                indicator.setFraction(0.1);
                indicator.setText(tp.getName() + " - test sets...");

            // Per scan, not a field: one scanner is built per project and reused
            // for every rescan, so a field would carry the last pass's folders
            // into this one.
            final @NotNull List<Path> unread = new ArrayList<>();

            final @NotNull TestCasesMainDirectoryDto tcd = tp.getTestCasesDirectory();
            store.getTestCasesMainDirsByPath().put(tcd.getPath().toString(), tcd);
            scanTestSets(tcd.getPath(), tcd, indicator, unread);

                indicator.setFraction(0.5);
                indicator.setText(tp.getName() + " - test runs...");

            final @NotNull TestRunsMainDirectoryDto trd = tp.getTestRunsDirectory();
            store.getTestRunsMainDirsByPath().put(trd.getPath().toString(), trd);
            scanTestRunDirs(trd.getPath(), trd, indicator, unread);

                indicator.setFraction(1.0);
                indicator.setText(tp.getName() + " - done.");

            reportUnread(tp.getName(), unread);

        } catch (final Exception ex) {
            Logger.error("Failed to scan project: " + projectPath.getFileName() + " - " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008 and Rule-INTERNAL-015
    private void scanTestSets(final @NotNull Path tcDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread) {
        try (Stream<Path> paths = Files.list(tcDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // Between test sets, because that is where the tester's Cancel
                // has to land: a project is thousands of files and the pass is
                // long enough to want stopping. Asked rather than thrown -
                // stopping is an answer, so there is no exception for every
                // caller above to sort back out from a real failure.
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TS) scanTestSet(dirPath, parent, indicator);
                            else scanTestSetPackage(dirPath, parent, indicator, unread);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_CASES, "test cases", unread));
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test sets: " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-008 and Rule-INTERNAL-015
    private void scanTestSetPackage(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestSetPackageDirectoryDto tsp = dirMapper.getTestSetPackageNode(p, path, parent);

            store.getTestSetPackagesByPath().put(path.toString(), tsp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            // The else was missing here, so a folder skipped one
                            // level down said nothing at all, not even to the log.
                            store.markedAs(subPath, DirectoryType.UNDER_TEST_CASES).ifPresentOrElse(marked -> {
                                if (marked == DirectoryType.TS) scanTestSet(subPath, tsp, indicator);
                                else scanTestSetPackage(subPath, tsp, indicator, unread);
                            }, () -> skipped(subPath, DirectoryType.UNDER_TEST_CASES, "test cases", unread));
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

    // UC-INTERNAL-002, Rule-INTERNAL-010 and Rule-INTERNAL-015
    private void scanTestRunDirs(final @NotNull Path trDir, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread) {
        try (Stream<Path> paths = Files.list(trDir)) {
            final @NotNull List<Path> dirs = paths.filter(Files::isDirectory).toList();

            for (final Path dirPath : dirs) {
                // The same stopping point on the run side, for the same reason.
                if (indicator.isCanceled()) return;

                store.markedAs(dirPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(
                        marked -> {
                            if (marked == DirectoryType.TR) scanTestRun(dirPath, parent, indicator);
                            else scanTestRunPackageDir(dirPath, parent, indicator, unread);
                        },
                        () -> skipped(dirPath, DirectoryType.UNDER_TEST_RUNS, "test runs", unread));
            }
        } catch (final Exception ex) {
            Logger.error("Failed to list test runs: " + ex.getMessage());
        }
    }

    // UC-INTERNAL-002, Rule-INTERNAL-010 and Rule-INTERNAL-015
    private void scanTestRunPackageDir(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator, final @NotNull List<Path> unread) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunPackageDirectoryDto trp = dirMapper.getTestRunPackageNode(p, path, parent);

            store.getTestRunPackagesByPath().put(path.toString(), trp);

            try (Stream<Path> subPaths = Files.list(path)) {
                subPaths.filter(Files::isDirectory)
                        .forEach(subPath -> {
                            // The else was missing here too.
                            store.markedAs(subPath, DirectoryType.UNDER_TEST_RUNS).ifPresentOrElse(marked -> {
                                if (marked == DirectoryType.TR) scanTestRun(subPath, trp, indicator);
                                else scanTestRunPackageDir(subPath, trp, indicator, unread);
                            }, () -> skipped(subPath, DirectoryType.UNDER_TEST_RUNS, "test runs", unread));
                        });
            }

        } catch (final Exception ex) {
            Logger.error("Failed to scan test run package: " + path.getFileName());
        }
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-015.
     * <p>
     * A folder with no marker is not a node, so the scan cannot read it or
     * anything under it. Most of them are nothing: a folder somebody made beside
     * the test sets, a working directory, something a tool left behind. Those are
     * the ordinary case and saying anything about them would be noise.
     * <p>
     * A folder holding test cases is not the ordinary case. Those cases are on
     * disk and in no panel, no search, no report and no export, and until now the
     * only trace was one line in a log nothing points at - four of them sat in the
     * sandbox project that way, and one in a real data root (#276).
     * <p>
     * So the log line is kept for every skip, and the folder is remembered only
     * when it holds something the tester would miss.
     */
    private void skipped(final @NotNull Path dirPath, final @NotNull List<DirectoryType> family, final @NotNull String where, final @NotNull List<Path> unread) {
        Logger.warn("Skipping unmarked directory under " + where + " (missing " + DirectoryType.markerNames(family) + "): " + dirPath);

        if (holdsTestCases(dirPath)) unread.add(dirPath);
    }

    /**
     * One listing of a folder the scan was about to throw away, so it costs
     * nothing on the folders that are really nodes.
     */
    private boolean holdsTestCases(final @NotNull Path dirPath) {
        try (Stream<Path> files = Files.list(dirPath)) {
            return files.filter(Files::isRegularFile).anyMatch(ProjectIndexer::isCaseFile);
        } catch (final Exception unreadable) {
            // A folder that will not even list is a bigger problem than a missing
            // marker, and the line above already said the scan skipped it.
            return false;
        }
    }

    /**
     * UC-INTERNAL-002, Rule-INTERNAL-015.
     * <p>
     * One notification for the whole project, not one per folder, and it stays in
     * the log rather than fading: a scan finishes on its own time, and a balloon
     * that fades while the tester is reading something else is no better than the
     * silence it replaced.
     */
    private void reportUnread(final @NotNull String projectName, final @NotNull List<Path> unread) {
        if (unread.isEmpty()) return;

        final @NotNull String named = unread.stream().limit(5).map(path -> path.getFileName().toString()).collect(Collectors.joining(", "));
        final @NotNull String rest = unread.size() > 5 ? ", and " + (unread.size() - 5) + " more" : "";
        final @NotNull String count = unread.size() == 1 ? "One folder holds" : unread.size() + " folders hold";

        Services.getInstance(p, Notifier.class).warn(p, "Folders not read in " + projectName,
                count + " test cases and carry no marker, so nothing in them was read: " + named + rest
                        + ". Create a test set of that name, or move the test cases into one.");
    }

    private void scanTestRun(final @NotNull Path path, final @NotNull DirectoryDto parent, final @NotNull ProgressIndicator indicator) {
        try {
            final @NotNull DirectoryMapper dirMapper = Services.getInstance(p, DirectoryMapper.class);
            final @NotNull TestRunDirectoryDto tr = dirMapper.getTestRunNode(p, path, parent);

            store.getTestRunsDirByPath().put(path.toString(), tr);

            final @NotNull Path jsonPath = TestRunDirectoryDto.resultsFile(path);
            if (Files.exists(jsonPath)) {
                final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
                final @NotNull TestRunDto trr = mapper.readValue(jsonPath.toFile(), TestRunDto.class);
                trr.dropStampsWithoutVerdict();
                store.getTestRunsByPath().put(path.toString(), trr);
            }

            indicator.setText("Test run: " + path.getFileName());

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
