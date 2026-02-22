package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.util.Notifier;
import testGit.util.Runner.TestNGRunnerByClassName;
import testGit.util.Tools;

public class RunTestCase extends DumbAwareAction {
    TestCase tc;

    public RunTestCase(TestCase tc) {
        super("Run Test", "", AllIcons.Nodes.RunnableMark);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String ref = tc.getAutomationRef();
        Project project = Config.getProject();
        if (ref != null && !ref.isBlank()) {
            Tools.printTestSourceRoots(project);
            TestNGRunnerByClassName.runTestClass(project, ref);
            Notifier.information("Running TestNG class: ", ref);
        } else {
            System.out.println("No automation reference found for this test case.");
            Notifier.warning("Note", "No automation reference found for this test case.");
        }
    }
}
