package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ViewCommitsAction extends AnAction {
    public ViewCommitsAction() {
        super("📌 View Pending Commits");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Open commit log UI
    }
}