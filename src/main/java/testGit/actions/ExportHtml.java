package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportHtml extends AnAction {
    public ExportHtml() {
        super("Export as HTML", "", AllIcons.FileTypes.Html);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: TO Be Implement export logic to HTML
    }
}
