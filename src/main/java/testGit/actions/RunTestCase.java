package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCase;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;
import testGit.util.Runner.TestNGRunnerByMethod;
import testGit.util.Tools;

public class RunTestCase extends DumbAwareAction {
    private final JBList<TestCase> list;

    public RunTestCase(final JBList<TestCase> list) {
        super("Run Test", "", AllIcons.RunConfigurations.TestState.Run);
        this.list = list;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.getShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCase tc = list.getSelectedValue();

        TestNGRunnerByMethod.runTestMethod(tc.getAutoRef(), Tools.toCamelCase(tc.getTitle()));
        Notifier.info("Running Test Case: ", tc.getTitle());
    }
}
