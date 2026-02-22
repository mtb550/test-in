package testGit.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ImportExcel extends DumbAwareAction {
    public ImportExcel() {
        super("From Excel");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Import test cases From Excel
    }
}
