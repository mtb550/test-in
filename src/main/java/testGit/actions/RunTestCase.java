package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.KeyboardSet;
import testGit.util.Tools;
import testGit.util.notifications.Notifier;
import testGit.util.runner.TestNGRunnerByMethod;

public class RunTestCase extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public RunTestCase(final JBList<TestCaseDto> list) {
        super("Run Test", "", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.getShortcut(), list);
    }

    public static void execute(final TestCaseDto tc) {
        if (tc == null) return;
        if ("RUNNING".equals(tc.getTempStatus())) return;

        TestNGRunnerByMethod.runTestMethod(tc.getFqcn(), Tools.toCamelCase(tc.getDescription()));
        Notifier.info("Running Test Case: ", tc.getDescription());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        execute(list.getSelectedValue());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
