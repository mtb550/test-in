package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectAction;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.enums.TestStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testRun.createDialog.FailedResultDialog;
import org.testin.util.Shortcuts;


public class UpdateRunItemAction extends AbstractProjectAction {
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public UpdateRunItemAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Failed Test Case Details", "Edit the failure details of the failed test case", AllIcons.Actions.Edit);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.UpdateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final @Nullable TestCaseDto selected = list.getSelectedValue();
        if (selected == null) return;

        if (!(editor instanceof RunEditor runEditor)) return;

        final @Nullable TestRunItems runItem = runEditor.getResultsMap().get(selected.getId());
        if (runItem == null) return;

        Logger.trace("update test run item for: " + selected.getDescription());

        // The same details dialog that opens automatically on a Failed status;
        // F2 edits without touching the status.
        new FailedResultDialog(p, runItem, () -> {
            final TestRunDto tr = runEditor.getTr();

            // The editor nulls the run while it reloads. Persisting is the whole point
            // of the callback, so say the edit was dropped rather than lose it quietly.
            if (tr == null) {
                Logger.warn("Run item edited while the run was reloading; not persisted");
                return;
            }

            Services.getInstance(p, ProjectIndexer.class).persistRun(runEditor.getParent().getPath(), tr);
            list.repaint();

            // After the persist and past the reload guard above: an edit that was
            // dropped rather than saved must not report itself as saved (#62).
            Services.getInstance(p, Notifier.class).softShow(p, "Details updated");
        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Details belong to failed test cases only - the dialog's title stays
        // truthful and the action reads as what it is.
        boolean enabled = false;
        final @Nullable TestCaseDto selected = list.getSelectedValue();
        if (selected != null && list.getSelectedValuesList().size() == 1 && editor instanceof RunEditor runEditor) {
            final @Nullable TestRunItems item = runEditor.getResultsMap().get(selected.getId());
            enabled = item != null && item.getStatus() == TestStatus.FAILED;
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads Swing selection and editor state - EDT only.
        return ActionUpdateThread.EDT;
    }
}
