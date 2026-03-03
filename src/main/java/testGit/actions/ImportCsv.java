package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ImportCsv extends DumbAwareAction {
    public ImportCsv() {
        super("From CSV");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// TODO: Import test cases From CSV
    }
}
