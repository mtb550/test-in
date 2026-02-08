package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import testGit.projectPanel.ProjectPanel;

public class RefreshAction extends AnAction {
    private final ProjectPanel projectPanel;

    public RefreshAction(final ProjectPanel projectPanel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("RefreshAction.actionPerformed()");

        // سيقوم بمسح القديم، تحميل الجديد من القرص، واختيار "All Projects"
        projectPanel.getProjectSelector().reloadProjects();
    }

}
