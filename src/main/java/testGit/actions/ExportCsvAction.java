package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportCsvAction extends AnAction {
    public ExportCsvAction() {
        super("Export as CSV");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Export test cases to CSV
    }
}
