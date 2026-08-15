package org.testin.creator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.git.GitRefs;
import org.testin.creator.dialogs.CreateProjectDialog;
import org.testin.explorer.ProjectPanel;
import org.testin.services.Services;
import org.testin.setting.Setting;
import org.testin.testproject.CreateTestProjectCloneAction;
import org.testin.testproject.CreateTestProjectNewAction;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

public class CreateTestProjectAction extends AbstractProjectAction {
    private final @NotNull ProjectPanel pp;

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull ProjectPanel pp) {
        super(p, "New Test Project", "Create or Clone test project", AllIcons.General.Add);
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

        new CreateProjectDialog(p, name -> {
            // What was typed decides: a repository URL is cloned, anything else
            // is a name for a new project.
            if (!GitRefs.isRepositoryUrl(name)) {
                new CreateTestProjectNewAction(p, pp, name).execute();
                return;
            }

            if (!OptionalPlugin.GIT.isAvailableOrWarn(p)) return;

            final String projectName = Services.getInstance(p, Tools.class).extractProjectNameFromUrl(name);
            new CreateTestProjectCloneAction(p, name, projectName, pp).execute();

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
