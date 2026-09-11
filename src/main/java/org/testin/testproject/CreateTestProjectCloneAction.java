package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import git4idea.commands.Git;
import git4idea.commands.GitCommandResult;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;

public class CreateTestProjectCloneAction extends AbstractProjectAction {
    private final @NotNull String gitUrl;
    private final @NotNull String projectName;
    private final @NotNull TreePanel tp;

    public CreateTestProjectCloneAction(final @NotNull Project p, final @NotNull String gitUrl, final @NotNull String name, final @NotNull TreePanel tp) {
        super(p, Bundle.message("clone.action.text"), Bundle.message("clone.action.description"), AllIcons.Vcs.Clone);
        this.gitUrl = gitUrl;
        this.projectName = name;
        this.tp = tp;
    }

    // UC-TREE-PANEL-003
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * UC-TREE-PANEL-003, Rule-TREE-PANEL-019.
     * <p>
     * Direct entry point for dialog callbacks — no AnActionEvent required.
     */
    public void execute() {

        if (gitUrl.trim().isEmpty() || projectName.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("clone.error.title"), Bundle.message("clone.error.missing"));
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, Bundle.message("clone.task"), false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(Bundle.message("clone.progress", projectName));

                try {
                    final @NotNull Path parentPath = Services.getInstance(p, TestinRoot.class).getPath();
                    final @NotNull GitCommandResult result = Git.getInstance().clone(p, parentPath, gitUrl, projectName);
                    result.throwOnError();

                    ApplicationManager.getApplication().invokeLater(() -> {
                        // The indexer owns disk reads/refresh: scanSingleProject re-scans the cloned
                        // project from disk. No direct VFS refresh here.
                        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                        final @NotNull Path projectPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(projectName);

                        indexer.scanSingleProject(projectPath);

                        // Bound to what was just cloned, for the same reason a new
                        // project is: this repository asked for it (#8).
                        Services.getInstance(p, BoundTestProject.class).bind(projectName);

                        tp.refresh();
                        Services.getInstance(p, Notifier.class).softShow(p, Done.CLONED);
                    });

                } catch (final Exception ex) {
                    Services.getInstance(p, Notifier.class).error(p, Bundle.message("clone.failed.title"), Bundle.message("clone.failed.message", ex.getMessage()));
                }
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
