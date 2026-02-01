package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class TestPlansAction extends AnAction {
    public TestPlansAction() {
        super("🧪 Test Plans");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Messages.showInfoMessage("Test Plans feature coming soon!", "Info");
        // TODO: You can replace the above with real logic when ready.
    }
}
