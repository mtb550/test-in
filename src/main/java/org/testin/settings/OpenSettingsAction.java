package org.testin.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class OpenSettingsAction extends DumbAwareAction {
    private final @NotNull Project p;

    public OpenSettingsAction(final @NotNull Project p) {
        super("Settings", "Configure Testin settings", AllIcons.General.Settings);
        this.p = p;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(p, SettingsConfigurable.class);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}