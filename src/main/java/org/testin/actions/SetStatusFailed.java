package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.IEditorUI;
import org.testin.pojo.TestStatus;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.services.RunStatusService;
import org.testin.util.services.Services;

public class SetStatusFailed extends DumbAwareAction {
    private final IEditorUI ui;
    private final JBList<TestCaseDto> list;

    public SetStatusFailed(final IEditorUI ui, final JBList<TestCaseDto> list) {
        super("Failed", "Set test case status to Failed", AllIcons.Actions.Cancel);
        this.ui = ui;
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.SetStatusFailed.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        Services.getInstance(project, RunStatusService.class).applyStatus(project, ui, list, TestStatus.FAILED);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
