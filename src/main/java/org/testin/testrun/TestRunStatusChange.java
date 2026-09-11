package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.editor.run.RunEditor;
import org.testin.explorer.TreePanel;
import org.testin.logger.Logger;
import org.testin.model.TestRunStatus;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;

import java.util.Optional;

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
 * <p>
 * It was written twice again, in two places this time: the run editor came here
 * and the tree's Set Status menu did the same job for itself, so each redrew the
 * surface it was standing on and neither told the other. The status is set on
 * the run, not on whoever is looking at it, so it is set here and every surface
 * showing that run is told (#191).
 */
@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestRunStatusChange {

    private final @NotNull Project p;

    /**
     * UC-TREE-PANEL-020, Rule-EDITOR-PANEL-008, Rule-TREE-PANEL-091.
     * <p>
     * Moves this run to a new status.
     * <p>
     * Completing stops the execution, whatever the run was before. That covers
     * both routes the two old methods took: a run that finished on its own, and
     * a run marked completed while in progress.
     * <p>
     * The editor is looked up rather than passed. The tree has no editor to
     * hand over and did not need one to change a status - what it needed was for
     * the editor to hear about it - and the run editor's own three call sites
     * are the same run found the same way.
     */
    public void apply(final @NotNull TestRunDirectoryDto run, final @NotNull TestRunStatus newStatus) {
        final @NotNull Optional<RunEditor> open = openEditorOn(run);

        Logger.trace("Test run status changed: " + run.getName() + " = " + newStatus.getLabel());

        if (newStatus == TestRunStatus.COMPLETED) open.ifPresent(RunEditor::stopExecution);

        // On the caller's own copy as well as the indexer's: persistMarker
        // updates the run the indexer holds, and an editor opened earlier can be
        // holding another instance of it.
        run.getMarker().setStatus(newStatus);

        persist(run, open);
        redraw(open);

        // The status names itself. Start Run routes through here rather than
        // notifying for itself, so pressing it says "In Progress" once (#62).
        Services.getInstance(p, Notifier.class).softShow(p, newStatus.getLabel());
    }

    /**
     * The run editor open on this run, and empty when nothing has it open - the
     * tree's usual case, and the editor's never.
     */
    private @NotNull Optional<RunEditor> openEditorOn(final @NotNull TestRunDirectoryDto run) {
        return Services.getInstance(p, TestinEditors.class).editorFor(p, run)
                .filter(RunEditor.class::isInstance)
                .map(RunEditor.class::cast);
    }

    /**
     * Both writes go through the single-writer RunStatusService: state is
     * snapshotted on the EDT, so later clicks can never tear the persisted JSON.
     * <p>
     * The run itself is written only when an editor is holding it, because that
     * is the only way it can have changed - the execution start stamp the Start
     * Run button sets just before calling here. With no editor, the marker is
     * the whole of what moved, and persistMarker already writes what a terminal
     * status does to the cases.
     */
    private void persist(final @NotNull TestRunDirectoryDto run, final @NotNull Optional<RunEditor> open) {
        final @NotNull RunStatusService statusService = Services.getInstance(p, RunStatusService.class);

        statusService.persistMarker(p, run.getPath(), run.getMarker().getStatus());
        open.ifPresent(editor -> statusService.persistRun(p, editor));
    }

    /**
     * UC-TREE-PANEL-020, Rule-TREE-PANEL-091.
     * <p>
     * Every surface showing this run: the tree row that names its status, and
     * the editor, whose verdict counts move with it - completing a run turns
     * every pending case untested.
     * <p>
     * The tree is asked for only when this project has one. An editor restored
     * on startup can outlive the tool window being opened, and building a tree
     * panel for a project that never asked for one is the thing #77 exists to
     * stop.
     */
    private void redraw(final @NotNull Optional<RunEditor> open) {
        ApplicationManager.getApplication().invokeLater(() -> open.ifPresent(RunEditor::refreshAfterRunStatusChanged));

        if (Services.isNotCreated(p, TreePanel.class)) return;
        Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
    }
}
