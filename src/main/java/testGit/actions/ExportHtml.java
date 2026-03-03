package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class ExportHtml extends DumbAwareAction {
    public ExportHtml() {
        super("Export as HTML", "", AllIcons.FileTypes.Html);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /// TODO: TO Be Implement export logic to HTML
    }
}
