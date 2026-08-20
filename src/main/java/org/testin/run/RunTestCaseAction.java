package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Shortcuts;


public class RunTestCaseAction extends AbstractProjectAction {

    private final @NotNull JBList<TestCaseDto> list;

    public RunTestCaseAction(final @NotNull Project p, final @NotNull JBList<TestCaseDto> list) {
        super(p, "Run Test", "Run selected test cases", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.RunTestCase.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        RunTestCases.run(p, list.getSelectedValuesList());
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
