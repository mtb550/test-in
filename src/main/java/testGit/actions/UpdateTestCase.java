package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;

public class UpdateTestCase extends DumbAwareAction {
    TestCase tc;

    public UpdateTestCase(TestCase tc) {
        super("Update", "Update test case", AllIcons.Actions.Edit);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ///  to be implemented
        /// show view details in view panel
    }
}

