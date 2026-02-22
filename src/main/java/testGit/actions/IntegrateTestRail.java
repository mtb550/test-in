package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class IntegrateTestRail extends DumbAwareAction {
    public IntegrateTestRail() {
        super("From Test Rail");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: From Test Rail
    }
}
