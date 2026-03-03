package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class OpenOldVersions extends DumbAwareAction {
    public OpenOldVersions() {
        super("Open Old Versions", "", AllIcons.Actions.SearchWithHistory);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// TODO: Load old test case versions
    }
}
