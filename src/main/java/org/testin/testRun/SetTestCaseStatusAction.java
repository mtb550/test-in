package org.testin.testRun;

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
import org.testin.util.Tools;

import java.util.List;

/**
 * Sets the selected test cases to any user-settable {@link TestStatus}. The
 * constant carries the label, icon, shortcut and whether details are collected
 * first — one action for all statuses instead of one class per status.
 */
public class SetTestCaseStatusAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull TestStatus status;

    public SetTestCaseStatusAction(final @NotNull Project p, final @NotNull IEditor editor,
                                   final @NotNull JBList<TestCaseDto> list, final @NotNull TestStatus status,
                                   final @NotNull TestStatus.MenuEntry entry) {
        super(status.getLabel(), "Set test case status to " + status.getLabel(), entry.icon());
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.status = status;

        this.registerCustomShortcutSet(Tools.customShortcut(entry.shortcut()), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        // Single selection of a run item: collect failure details first, apply after the dialog closes.
        if (status.isCollectsFailureDetails() && editor instanceof RunEditor runEditor && selectedItems.size() == 1) {
            final TestRunItems runItem = runEditor.getResultsMap().get(selectedItems.getFirst().getId());
            if (runItem != null) {
                new FailedResultDialog(p, runItem, this::applyStatus).show();
                return;
            }
        }

        applyStatus();
    }

    private void applyStatus() {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, status);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
