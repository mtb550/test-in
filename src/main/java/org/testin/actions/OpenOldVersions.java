package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class OpenOldVersions extends DumbAwareAction {
    public OpenOldVersions() {
        super("Open Old Versions", "", AllIcons.Actions.SearchWithHistory);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        /// TODO: Load old test case versions
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
