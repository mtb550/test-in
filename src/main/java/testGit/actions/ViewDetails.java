package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.viewPanel.ViewPanel;

public class ViewDetails extends DumbAwareAction {
    TestCase tc;

    public ViewDetails(TestCase tc) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ViewPanel.show(tc);
    }
}
