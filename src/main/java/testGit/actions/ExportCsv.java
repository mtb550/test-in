package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ExportCsv extends DumbAwareAction {
    public ExportCsv() {
        super("Export as CSV", "", AllIcons.FileTypes.Text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Export test cases to CSV
    }
}
