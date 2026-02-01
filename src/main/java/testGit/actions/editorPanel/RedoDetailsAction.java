package testGit.actions.editorPanel;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.util.ActionHistory;

public class RedoDetailsAction extends AnAction {
    TestCase tc;

    public RedoDetailsAction(TestCase tc) {
        super("↪ Redo");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ActionHistory.redo();
    }
}
