package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.viewPanel.ViewPanel;

public class ViewDetails extends DumbAwareAction {
    TestCaseJsonMapper tc;

    public ViewDetails(TestCaseJsonMapper tc) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.tc = tc;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ViewPanel.show(tc);
    }
}
