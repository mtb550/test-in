package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;
import testGit.util.Runner.TestNGRunnerByMethod;
import testGit.util.Tools;

public class RunTestCase extends DumbAwareAction {
    TestCase tc;

    public RunTestCase(TestCase tc, JBList<TestCase> list) {
        super("Run Test", "", AllIcons.RunConfigurations.TestState.Run);
        this.tc = tc;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.get(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestNGRunnerByMethod.runTestMethod(tc.getAutoRef(), Tools.toCamelCase(tc.getTitle()));
        Notifier.info("Running Test Case: ", tc.getTitle());
    }
}
