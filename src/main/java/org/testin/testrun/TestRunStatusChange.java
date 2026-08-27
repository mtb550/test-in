package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunEditor;
import org.testin.logger.Logger;
import org.testin.model.TestRunStatus;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;

/**
 * Moving a test run to a new status: writing it, persisting it, redrawing what
 * shows it, and saying so once.
 * <p>
 * This was an action - {@code UpdateTestRunStatusAction} - registered nowhere
 * and reached only by four call sites that constructed one to call its methods
 * directly. Its {@code actionPerformed} could not run at all, which mattered
 * beyond being dead: it was the only caller of the status enum's transition
 * table, so the lifecycle that table declared was enforced nowhere. The table
 * went with it.
 * <p>
 * Completing a run was written twice in that class. The two differed by a guard
 * that could not fire and a log line one of them was missing, so they were the
 * same thing today and the next change to how a run completes would have landed
 * in one of them.
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestRunStatusChange {

    private final @NotNull Project p;

    /**
     * Moves this run to a new status.
     * <p>
     * Completing stops the execution, whatever the run was before. That covers
     * both routes the two old methods took: a run that finished on its own, and
     * a run marked completed while in progress.
     */
    public void apply(final @NotNull RunEditor editor, final @NotNull TestRunStatus newStatus) {
        final @NotNull TestRunMarker marker = editor.getParent().getMarker();

        marker.setStatus(newStatus);

        Logger.trace("Test run status changed: " + editor.getParent().getName() + " = " + newStatus.getLabel());

        if (newStatus == TestRunStatus.COMPLETED) editor.stopExecution();

        persist(editor, marker);

        ApplicationManager.getApplication().invokeLater(editor::refreshAfterRunStatusChanged);

        // The status names itself. Start Run routes through here rather than
        // notifying for itself, so pressing it says "In Progress" once (#62).
        Services.getInstance(p, Notifier.class).softShow(p, newStatus.getLabel());
    }

    /**
     * Both writes go through the single-writer RunStatusService: state is
     * snapshotted on the EDT, so later clicks can never tear the persisted JSON.
     */
    private void persist(final @NotNull RunEditor editor, final @NotNull TestRunMarker marker) {
        final @NotNull RunStatusService statusService = Services.getInstance(p, RunStatusService.class);

        statusService.persistMarker(p, editor.getParent().getPath(), marker.getStatus());
        statusService.persistRun(p, editor);
    }
}
