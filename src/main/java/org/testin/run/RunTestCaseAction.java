package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.KeyboardSet;
import org.testin.util.broadcasts.listeners.ITestCaseExecutionListener;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestNGRunnerByMethod;
import org.testin.util.services.Services;

import java.util.List;

public class RunTestCaseAction extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public RunTestCaseAction(final JBList<TestCaseDto> list) {
        super("Run Test", "Run selected test cases", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;

        for (TestCaseDto tc : testCases) {
            if (tc == null || "RUNNING".equals(tc.getTempStatus())) continue;

            p.getMessageBus().syncPublisher(ITestCaseExecutionListener.TOPIC).onStatusChanged(tc.getId().toString().toLowerCase(), "RUNNING", null);

            Services.getInstance(p, Notifier.class).softShow(p, "Running Test Case: ", tc.getDescription());
            Services.getInstance(p, TestNGRunnerByMethod.class).runTestMethod(p, tc);
        }
    }

    public void execute(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        execute(p, List.of(tc));
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;

        List<TestCaseDto> selectedValues = list.getSelectedValuesList();
        execute(e.getProject(), selectedValues);
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
