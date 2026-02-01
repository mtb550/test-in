package testGit.actions.editorPanel;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.viewPanel.TestCaseToolWindow;

public class ViewDetailsAction extends AnAction {
    TestCase tc;

    public ViewDetailsAction(TestCase tc) {
        super("🔍 View Details");
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseToolWindow.show(tc);
    }
}
