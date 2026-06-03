package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.CodeNavigator;
import org.testin.util.KeyboardSet;

public class NavigateToCode extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public NavigateToCode(final JBList<TestCaseDto> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.NavigateToCode.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project project, final TestCaseDto tc) {
        if (tc == null || tc.getFqcn().isEmpty()) return;
        new CodeNavigator().toCode(project, tc.getFqcn());
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        if (e == null) return;
        execute(e.getProject(), list.getSelectedValue());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}