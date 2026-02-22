package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ExportExcel extends DumbAwareAction {
    public ExportExcel() {
        super("Export as Excel", "", AllIcons.FileTypes.MicrosoftWindows);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Implement export logic to EXCEL
    }
}
