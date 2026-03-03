package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ImportJson extends DumbAwareAction {
    public ImportJson() {
        super("From Json");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// TODO: Import test cases From Json
    }
}
