package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportJsonAction extends AnAction {
    public ExportJsonAction() {
        super("Export as Json");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Implement export logic to JSON
    }
}
