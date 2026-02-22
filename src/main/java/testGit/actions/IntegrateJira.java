package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateJira extends DumbAwareAction {
    public IntegrateJira() {
        super("From Jira");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: From Jira
    }
}
