package org.testin.indexer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.logger.Logger;
import org.testin.services.Services;
import org.testin.util.EditorUtil;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Brings the index back in step with the disk, once for a burst of changes
 * (#20).
 * <p>
 * A pull, a branch switch or an unpack fires hundreds of events in a second. A
 * scan per event would re-read the same project hundreds of times and rebuild
 * the tree under the tester's hands while it did; so the projects that changed
 * are collected and read once, a moment after the last of them lands.
 * <p>
 * Separate from the listener that feeds it because they are two jobs: deciding
 * what changed has to be cheap and runs on every event in the IDE, while this
 * reads files and redraws and must not.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.APP)
public final class Rescan {

    /**
     * How long after the last change the scan runs.
     * <p>
     * Long enough that a burst arrives as one, short enough that a tester who
     * edited a file in another window and switched back does not notice waiting.
     */
    private static final long QUIET_MILLIS = 400;

    private final @NotNull Set<Path> waiting = ConcurrentHashMap.newKeySet();

    /**
     * Whether a pass is already booked. The whole point of the delay is that the
     * second, tenth and hundredth change of a burst join the pass the first one
     * booked instead of booking their own.
     */
    private final @NotNull AtomicBoolean booked = new AtomicBoolean();

    public void of(final @NotNull Collection<Path> testProjects) {
        if (testProjects.isEmpty()) return;

        waiting.addAll(testProjects);
        if (!booked.compareAndSet(false, true)) return;

        AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(this::run, QUIET_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void run() {
        // Released before the work, not after: a change that lands while this
        // pass is reading has not been read, and must book the next one.
        booked.set(false);

        final @NotNull List<Path> testProjects = List.copyOf(waiting);
        testProjects.forEach(waiting::remove);
        if (testProjects.isEmpty()) return;

        Logger.info("Something changed on disk in " + testProjects.size()
                + (testProjects.size() == 1 ? " test project" : " test projects") + ", reading them again");

        for (final Project p : ProjectManager.getInstance().getOpenProjects()) {
            refresh(p, testProjects);
        }
    }

    /**
     * One project's tree brought up to date, and only where there is a tree.
     * <p>
     * A project that never opened the Testin tool window has nothing on screen
     * to correct, and asking its service container for a panel would build one
     * and start indexing there - a refresh of something the tester never opened
     * (#77).
     */
    private void refresh(final @NotNull Project p, final @NotNull List<Path> testProjects) {
        if (p.isDisposed() || !Services.isCreated(p, ExplorerPanel.class)) return;

        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        testProjects.forEach(indexer::scanSingleProject);

        ApplicationManager.getApplication().invokeLater(() -> {
            if (p.isDisposed()) return;

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().refresh();

            // The tree is not the only thing showing what a file used to say. An
            // editor holds the node it was opened on and the cases it read from
            // it, so after the scan both can be wrong - and this is the same
            // call the Refresh button makes, so a change noticed on disk lands
            // exactly where a change the tester asked about lands.
            Services.getInstance(p, EditorUtil.class).refreshOpen(p);
        });
    }
}
