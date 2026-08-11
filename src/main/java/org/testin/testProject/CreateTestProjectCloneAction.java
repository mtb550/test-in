package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;

import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;
import org.testin.settings.Setting;

import java.nio.file.Path;

public class CreateTestProjectCloneAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull String gitUrl;
    private final @NotNull String projectName;
    private final @NotNull ProjectPanel pp;

    public CreateTestProjectCloneAction(final @NotNull Project p, final @NotNull String gitUrl, final @NotNull String name, final @NotNull ProjectPanel pp) {
        super("Clone Git Project", "Import an existing test project from Git", AllIcons.Vcs.Clone);
        this.p = p;
        this.gitUrl = gitUrl;
        this.projectName = name;
        this.pp = pp;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /** Direct entry point for dialog callbacks — no AnActionEvent required. */
    public void execute() {

        if (gitUrl.trim().isEmpty() || projectName.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Clone Error", "Missing parameters for cloning the project.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Cloning repository", false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Cloning into " + projectName + "...");

                try {
                    final Path parentPath = Services.getInstance(p, Setting.class).getTestinPath();
                    final GitCommandResult result = Git.getInstance().clone(p, parentPath, gitUrl, projectName);
                    result.throwOnError();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // The indexer owns disk reads/refresh: scanSingleProject re-scans the cloned
                        // project from disk. No direct VFS refresh here.
                        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                        final Path projectPath = Services.getInstance(p, Setting.class).getTestinPath().resolve(projectName);

                        indexer.scanSingleProject(projectPath);
                        final TestProjectDirectoryDto clonedProject = indexer.getTestProjectsByPath().get(projectPath.toString());
                        if (clonedProject != null)
                            pp.getTestProjectSelector().addTestProject(clonedProject);

                        pp.getProjectTree().updateNodes();
                        Services.getInstance(p, Notifier.class).info(p, "Clone Successful", "Project '" + projectName + "' was cloned successfully.");
                    });

                } catch (final Exception ex) {
                    Services.getInstance(p, Notifier.class).error(p, "Clone Failed", "Could not clone repository:\n" + ex.getMessage());
                }
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
