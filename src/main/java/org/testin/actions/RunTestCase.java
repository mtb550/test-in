package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestNGRunnerByMethod;

import java.util.List;

public class RunTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public RunTestCase(final JBList<TestCaseDto> list) {
        super("Run Test", "Run selected test cases", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project project, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        for (TestCaseDto tc : testCases) {
            if (tc == null || "RUNNING".equals(tc.getTempStatus())) continue;

            Notifier.getInstance().softShow(project, "Running Test Case: ", tc.getDescription());
            TestNGRunnerByMethod.runTestMethod(project, tc.getFqcn());
        }
    }

    public void execute(final @NotNull Project project, final @NotNull TestCaseDto tc) {
        execute(project, List.of(tc));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCaseDto> selectedValues = list.getSelectedValuesList();
        execute(e.getProject(), selectedValues);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}