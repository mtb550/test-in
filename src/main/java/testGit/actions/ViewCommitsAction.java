package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ViewCommitsAction extends AnAction {
    public ViewCommitsAction() {
        super("View Pending Commits", "", AllIcons.Actions.Commit);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Open commit log UI
    }
}