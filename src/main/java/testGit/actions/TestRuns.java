package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class TestRuns extends DumbAwareAction {
    public TestRuns() {
        super("Test Runs", "", AllIcons.Actions.ListFiles);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Messages.showInfoMessage("Test Runs feature coming soon!", "Info");
        /// TODO: You can replace the above with real logic when ready.
    }
}
