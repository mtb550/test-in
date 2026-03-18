package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.mappers.TestCase;
import testGit.viewPanel.ViewPanel;

public class ViewDetails extends DumbAwareAction {
    private final JBList<TestCase> list;

    public ViewDetails(final JBList<TestCase> list) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.list = list;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCase tc = list.getSelectedValue();
        ViewPanel.show(tc);
    }
}
