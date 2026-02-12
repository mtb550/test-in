package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class IntegrateJira extends AnAction {
    public IntegrateJira() {
        super("From Jira");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: From Jira
    }
}
