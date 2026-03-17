package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCaseJsonMapper;

public class RedoDetails extends DumbAwareAction {
    TestCaseJsonMapper tc;

    public RedoDetails(TestCaseJsonMapper tc) {
        super("↪ Redo");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ///  TO BE IMPLEMENTED
    }
}
