package org.testin.indexer;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Notices test data changing on disk and reads it again (#20).
 * <p>
 * Everything Testin shows comes from the indexer's cache, which was filled once
 * at startup. So a test case edited by hand, a branch switched from the IDE's
 * own widget, or a pull run outside the plugin left the tester looking at what
 * the file used to say, with nothing on screen admitting it. The only way back
 * was the Refresh button, which nobody presses after being told the sync
 * succeeded.
 * <p>
 * <b>This sees every file event in the IDE</b> - a build writing class files,
 * the platform's own indexes, every other plugin. So the filtering is arithmetic
 * on paths and nothing else, and answers "no" for almost everything:
 * {@link WatchedPath} decides, and it is tested on its own.
 */
public final class TestinFileWatcher implements AsyncFileListener {

    /**
     * Null means "nothing here concerns Testin", which is the answer for
     * virtually every batch. The platform's contract, and the reason this
     * method is the cheap half of the listener - the work is in the applier,
     * which only exists when something did concern us.
     */
    @Override
    public @Nullable ChangeApplier prepareChange(final @NotNull List<? extends VFileEvent> events) {
        final @NotNull Set<Path> testProjects = changedTestProjects(events);
        if (testProjects.isEmpty()) return null;

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                Services.getInstance(Rescan.class).of(testProjects);
            }
        };
    }

    /**
     * The test projects this batch of events touched, and empty when it touched
     * none.
     */
    private static @NotNull Set<Path> changedTestProjects(final @NotNull List<? extends VFileEvent> events) {
        final @NotNull Path root = TestinRoot.normalize(Services.getInstance(AppSettingsState.class).rootTestinPath);
        final @NotNull OwnWrites ours = Services.getInstance(OwnWrites.class);
        final @NotNull Set<Path> testProjects = new HashSet<>();

        for (final VFileEvent event : events) {
            changedFile(event)
                    // A file the plugin itself just saved is not news. Reading
                    // the project again for it would rebuild the tree under the
                    // hand of the tester who caused the save.
                    .filter(file -> !ours.areOurs(file))
                    .flatMap(file -> WatchedPath.testProjectOf(file, root))
                    .ifPresent(testProjects::add);
        }

        return testProjects;
    }

    /**
     * The path an event is about, and empty for one whose path this file system
     * cannot name - a jar entry, an in-memory file.
     * <p>
     * Asked of {@code getPath} rather than {@code getFile}: a file being created
     * has no {@link com.intellij.openapi.vfs.VirtualFile} yet, so {@code getFile}
     * answers null for exactly the additions a pull brings - and reading it drove
     * them straight to empty, leaving a branch that only added test cases
     * unnoticed until the tester pressed Refresh. The path is there for every
     * event; whether it is test data at all is {@link WatchedPath}'s to decide.
     */
    private static @NotNull Optional<Path> changedFile(final @NotNull VFileEvent event) {
        try {
            return Optional.of(Path.of(event.getPath()));
        } catch (final RuntimeException notAFileSystemPath) {
            return Optional.empty();
        }
    }
}
