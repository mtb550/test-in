package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;
import testGit.util.Runner.TestNGRunnerByClassName;
import testGit.util.Tools;

public class RunTestCase extends DumbAwareAction {
    TestCase tc;

    public RunTestCase(TestCase tc, JBList<TestCase> list) {
        super("Run Test", "", AllIcons.Nodes.RunnableMark);
        this.tc = tc;
        this.registerCustomShortcutSet(KeyboardSet.RunTestCase.get(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String ref = tc.getAutomationRef();
        Project project = Config.getProject();
        if (ref != null && !ref.isBlank()) {
            Tools.printTestSourceRoots(project);
            TestNGRunnerByClassName.runTestClass(project, ref);
            Notifier.info("Running TestNG class: ", ref);
        } else {
            System.out.println("No automation reference found for this test case.");
            Notifier.warn("Note", "No automation reference found for this test case.");
        }
    }
}
