package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.runEditor.RunEditor;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.testRun.createDialog.FailedResultDialog;
import org.testin.util.KeyboardSet;

import java.util.List;

public class SetTestCaseStatusFailedAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetTestCaseStatusFailedAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Failed", "Set test case status to Failed", AllIcons.Actions.Cancel);
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetStatusFailed.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (!(editor instanceof RunEditor runEditor)) {
            Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.FAILED);
            return;
        }

        if (selectedItems.size() == 1) {
            // Single selection: show dialog, apply status after it closes
            TestCaseDto tc = selectedItems.getFirst();
            TestRunItems runItem = runEditor.getResultsMap().get(tc.getId());
            if (runItem == null) {
                Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.FAILED);
                return;
            }

            FailedResultDialog dialog = new FailedResultDialog(p, runItem, () ->
                    Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.FAILED)
            );
            dialog.show();
        } else {
            // Multiple selections: apply status directly without dialog
            Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.FAILED);
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
