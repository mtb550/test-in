package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.projectPanel.ProjectPanel;

public class Refresh extends DumbAwareAction {
    private final ProjectPanel projectPanel;

    public Refresh(ProjectPanel projectPanel) {
        super("Refresh", "Reload tree", AllIcons.Actions.Refresh);
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        projectPanel.setupMainLayout();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        boolean hasTree = projectPanel.getProjectTree() != null && projectPanel.getProjectTree().getMainTree() != null;
        e.getPresentation().setEnabled(hasTree);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}