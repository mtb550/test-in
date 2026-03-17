package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCaseJsonMapper;

public class UndoDetails extends DumbAwareAction {
    TestCaseJsonMapper tc;

    public UndoDetails(TestCaseJsonMapper tc) {
        super("Undo", "", AllIcons.Actions.Undo);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ///  TO BE IMPLEMENTED
    }
}
