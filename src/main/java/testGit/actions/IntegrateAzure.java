package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateAzure extends DumbAwareAction {
    public IntegrateAzure() {
        super("From Azure DevOps");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// TODO: From Azure DevOps
    }
}
