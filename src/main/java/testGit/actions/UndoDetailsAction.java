package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.util.ActionHistory;

public class UndoDetailsAction extends AnAction {
    TestCase tc;

    public UndoDetailsAction(TestCase tc) {
        super("↩ Undo");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ActionHistory.undo();
    }
}
