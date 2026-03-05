package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.Nullable;
import testGit.projectPanel.ProjectPanel;

public class Refresh extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public Refresh(final ProjectPanel projectPanel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        System.out.println("RefreshAction.actionPerformed()");
        //projectPanel.getTestProjectSelector().loadTestProjectList();
        projectPanel.setupMainLayout();
    }

}
