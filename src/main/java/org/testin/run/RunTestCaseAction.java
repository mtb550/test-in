package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.enums.RunStatus;
import org.testin.listeners.TestCaseExecutionListener;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGRunnerByMethod;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;
import org.testin.util.Shortcuts;

import java.util.List;

public class RunTestCaseAction extends AbstractProjectAction {
    private final JBList<TestCaseDto> list;

    public RunTestCaseAction(final @NotNull Project p, final JBList<TestCaseDto> list) {
        super(p, "Run Test", "Run selected test cases", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(Shortcuts.RunTestCase.getCustomShortcut(), list);
    }

    public void execute(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases) {
        if (testCases.isEmpty()) return;
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        int started = 0;

        for (TestCaseDto tc : testCases) {
            if (tc == null || tc.getTempStatus() == RunStatus.RUNNING) continue;

            p.getMessageBus().syncPublisher(TestCaseExecutionListener.TOPIC).onStatusChanged(tc.getId().toString().toLowerCase(), RunStatus.RUNNING, null);

            Services.getInstance(p, TestNGRunnerByMethod.class).runTestMethod(p, tc);
            started++;
        }

        // Once for the click, not once per case: running a page of twelve used
        // to raise twelve balloons. Nothing is said when every case was already
        // running, because nothing was started.
        if (started == 1) Services.getInstance(p, Notifier.class).softShow(p, "Running");
        else if (started > 1) Services.getInstance(p, Notifier.class).softShow(p, "Running " + started);
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
