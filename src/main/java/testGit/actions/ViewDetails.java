package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;

public class ViewDetails extends DumbAwareAction {
    private final JBList<TestCaseDto> list;

    public ViewDetails(final JBList<TestCaseDto> list) {
        super("View Details", "", AllIcons.Actions.PreviewDetails);
        this.list = list;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        TestCaseDto tc = list.getSelectedValue();
        ViewPanel.show(tc);
    }
}
