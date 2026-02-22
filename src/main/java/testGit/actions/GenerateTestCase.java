package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

public class GenerateTestCase extends DumbAwareAction {
    private final TestCase tc;

    public GenerateTestCase(TestCase tc) {
        super("Generate Test", "", AllIcons.RunConfigurations.TestState.Run);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// to be implemented
        System.out.println(tc.getTitle());
    }
}
