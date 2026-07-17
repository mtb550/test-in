package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

// TODO: implement save as to allow tester to specify save place
public class ExportHtml extends DumbAwareAction {
    public ExportHtml() {
        super("Export as HTML", "", AllIcons.FileTypes.Html);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        /// TODO: TO Be Implement export logic to HTML
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
