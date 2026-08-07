package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.enums.TestStatus;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.util.KeyboardSet;

public class SetTestCaseStatusPassedAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetTestCaseStatusPassedAction(final @NotNull Project p, final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Passed", "Set test case status to Passed", AllIcons.Actions.Checked);
        this.p = p;
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetStatusPassed.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Services.getInstance(p, RunStatusService.class).applyStatus(p, editor, list, TestStatus.PASSED);
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
