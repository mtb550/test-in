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
import org.testin.util.Tools;
import org.testin.util.services.Services;

import java.util.ArrayList;

public class NavigateToCode extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public NavigateToCode(final JBList<TestCaseDto> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.NavigateToCode.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project project, final TestCaseDto tc) {
        ArrayList<String> generatedFqcn = Services.getInstance(project, Tools.class).buildFqcn(tc);
        new CodeNavigator().toCode(project, generatedFqcn);
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        //fixme: @Nullable is not acceptable by intellij
        if (e == null) return;
        if (e.getProject() == null) return;
        execute(e.getProject(), list.getSelectedValue());
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