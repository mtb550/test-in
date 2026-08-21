package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.editor.TestinEditor;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.testrun.create.FailedResultDialog;
import org.testin.util.ListValue;

import java.util.Optional;
import org.testin.util.Shortcuts;


public class UpdateRunItemAction extends AbstractProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateRunItemAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Failed Test Case Details", "Edit the failure details of the failed test case", AllIcons.Actions.Edit);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        // Nothing selected is nothing to edit.
        final Optional<TestCaseDto> selected = ListValue.selected(list);
        if (selected.isEmpty()) return;

        if (!(editor instanceof RunEditor runEditor)) return;

        final Optional<TestRunItems> found = runEditor.runItem(selected.orElseThrow().getId());
        if (found.isEmpty()) return;

        final TestRunItems runItem = found.orElseThrow();
        final TestCaseDto testCase = selected.orElseThrow();

        // The test case is gone: what the run recorded against it stands as it is.
        if (runItem.isRemoved()) {
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return;
        }

        Logger.trace("update test run item for: " + testCase.getDescription());

        // The same details dialog that opens automatically on a Failed status;
        // F2 edits without touching the status.
        new FailedResultDialog(p, runItem, () -> runEditor.run().ifPresentOrElse(tr -> {
            Services.getInstance(p, ProjectIndexer.class).persistRun(runEditor.getParent().getPath(), tr);
            list.repaint();

            // After the persist: an edit that was dropped rather than saved must
            // not report itself as saved (#62).
            Services.getInstance(p, Notifier.class).softShow(p, "Details updated");

            // The editor empties the run while it reloads. Persisting is the whole
            // point of the callback, so say the edit was dropped rather than lose it
            // quietly.
        }, () -> Logger.warn("Run item edited while the run was reloading; not persisted"))).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Details belong to failed test cases only - the dialog's title stays
        // truthful and the action reads as what it is.
        boolean enabled = false;
        final Optional<TestCaseDto> selected = ListValue.selected(list);
        if (selected.isPresent() && list.getSelectedValuesList().size() == 1 && editor instanceof RunEditor runEditor) {
            enabled = runEditor.runItem(selected.orElseThrow().getId())
                    .filter(item -> item.getStatus() == TestStatus.FAILED)
                    .isPresent();
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads Swing selection and editor state - EDT only.
        return ActionUpdateThread.EDT;
    }
}
