package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditor;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.services.RunStatusService;
import org.testin.util.services.Services;

public class SetStatusPassed extends DumbAwareAction {
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetStatusPassed(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Passed", "Set test case status to Passed", AllIcons.Actions.Checked);
        this.editor = editor;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetStatusPassed.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        Services.getInstance(project, RunStatusService.class).applyStatus(project, editor, list, TestStatus.PASSED);
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
