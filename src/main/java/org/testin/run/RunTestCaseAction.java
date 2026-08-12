package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.listeners.ITestCaseExecutionListener;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGRunnerByMethod;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;
import org.testin.util.Shortcuts;

import java.util.List;

public class RunTestCaseAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final JBList<TestCaseDto> list;

    public RunTestCaseAction(final @NotNull Project p, final JBList<TestCaseDto> list) {
        super("Run Test", "Run selected test cases", AllIcons.RunConfigurations.TestState.Run);
        this.p = p;
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.RunTestCase.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        for (TestCaseDto tc : testCases) {
            if (tc == null || "RUNNING".equals(tc.getTempStatus())) continue;

            p.getMessageBus().syncPublisher(ITestCaseExecutionListener.TOPIC).onStatusChanged(tc.getId().toString().toLowerCase(), "RUNNING", null);

            Services.getInstance(p, Notifier.class).softShow(p, "Running Test Case: ", tc.getDescription());
            Services.getInstance(p, TestNGRunnerByMethod.class).runTestMethod(p, tc);
        }
    }

    public void execute(final @NotNull TestCaseDto tc) {
        execute(p, List.of(tc));
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> selectedValues = list.getSelectedValuesList();
        execute(p, selectedValues);
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
