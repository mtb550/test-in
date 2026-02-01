package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportExcelAction extends AnAction {
    public ExportExcelAction() {
        super("Export as Excel");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Implement export logic to EXCEL
    }
}
