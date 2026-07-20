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
import org.testin.util.services.RunStatusService;
import org.testin.util.services.Services;

public class SetStatusPending extends DumbAwareAction {
    private final @NotNull IEditor editor;
    private final @NotNull JBList<TestCaseDto> list;

    public SetStatusPending(final @NotNull IEditor editor, final @NotNull JBList<TestCaseDto> list) {
        super("Pending", "Set test case status to Pending", AllIcons.Process.ProgressResume);
        this.editor = editor;
        this.list = list;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        Services.getInstance(project, RunStatusService.class).applyStatus(project, editor, list, TestStatus.PENDING);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
