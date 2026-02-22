package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ViewCommits extends DumbAwareAction {
    public ViewCommits() {
        super("View Pending Commits", "", AllIcons.Actions.Commit);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Open commit log UI
    }
}