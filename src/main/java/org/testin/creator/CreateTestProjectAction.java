package org.testin.creator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.creator.dialogs.CreateProjectDialog;
import org.testin.explorer.ExplorerPanel;
import org.testin.git.GitRefs;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.setting.TestinRoot;
import org.testin.testproject.CreateTestProjectCloneAction;
import org.testin.testproject.CreateTestProjectNewAction;
import org.testin.util.OptionalPlugin;

public class CreateTestProjectAction extends AbstractProjectAction {
    private final @NotNull ExplorerPanel pp;

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
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

            // The folder is named by testin.yml, never by the URL. A repository
            // called nafath-test-case is a place to clone from; what the project
            // is called is a decision, and it is written down once in the file
            // that travels with the repository - so the tree, the reports and
            // the server path all read the same name.
            final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

            if (projectName.isEmpty()) {
                Services.getInstance(p, Notifier.class).warn(p, "No Test Project Named",
                        "testin.yml must say which test project this repository is about before one can be "
                                + "cloned. Set testinProject in it, or pick a project with Select Test Project.");
                return;
            }

            new CreateTestProjectCloneAction(p, name, projectName, pp).execute();

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (!Services.getInstance(p, TestinRoot.class).isConfigured())
            e.getPresentation().setEnabled(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
