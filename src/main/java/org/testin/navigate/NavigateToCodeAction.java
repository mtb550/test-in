package org.testin.navigate;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.util.Shortcuts;
import org.testin.util.Tools;

import java.util.ArrayList;

public class NavigateToCodeAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final JBList<TestCaseDto> list;

    public NavigateToCodeAction(final @NotNull Project p, final JBList<TestCaseDto> list) {
        super("Navigate to Code", "Jump to the automated test case", AllIcons.General.ArrowRight);
        this.p = p;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.NavigateToCode.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        ArrayList<String> generatedFqcn = Services.getInstance(p, Tools.class).buildFqcnMethod(tc);
        new CodeNavigator().toCode(p, generatedFqcn);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute(p, list.getSelectedValue());
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