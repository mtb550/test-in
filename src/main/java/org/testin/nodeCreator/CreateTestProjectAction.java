package org.testin.nodeCreator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.nodeCreator.dialogs.CreateProjectDialog;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.settings.Setting;
import org.testin.testProject.CreateTestProjectCloneAction;
import org.testin.testProject.CreateTestProjectNewAction;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

public class CreateTestProjectAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull ProjectPanel pp;

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        super("New Test Project", "Create or Clone test project", AllIcons.General.Add);
        this.p = p;
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * Direct entry point for the project panel's empty state — no AnActionEvent required.
     */
    public void execute() {

        new CreateProjectDialog(p, (name, type) -> {
            if (name.trim().isEmpty()) return;

            if (type == DirectoryType.IMPORT_TP) {
                if (!OptionalPlugin.GIT.isAvailableOrWarn(p)) return;

                String projectName = Services.getInstance(p, Tools.class).extractProjectNameFromUrl(name);
                new CreateTestProjectCloneAction(p, name.trim(), projectName, pp).execute();
                return;
            }

            if (type == DirectoryType.TP) {
                new CreateTestProjectNewAction(p, pp, name.trim()).execute();
            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (Services.getInstance(p, Setting.class).getTestinPath().toString().isEmpty())
            e.getPresentation().setEnabled(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}