package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OpenOldVersionsAction extends AnAction {
    public OpenOldVersionsAction() {
        super("🕓 Open Old Versions");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Load old test case versions
    }
}
