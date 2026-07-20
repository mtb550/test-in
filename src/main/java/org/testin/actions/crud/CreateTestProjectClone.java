package org.testin.actions.crud;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.GitCommandRunner;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import java.nio.file.Path;

public class CreateTestProjectClone extends DumbAwareAction {

    private final @NotNull String gitUrl;
    private final @NotNull String projectName;
    private final @NotNull ProjectPanel projectPanel;

    public CreateTestProjectClone(final @NotNull String gitUrl, final @NotNull String name, final @NotNull ProjectPanel projectPanel) {
        super("Clone Git Project", "Import an existing test project from Git", AllIcons.Vcs.Clone);
        this.gitUrl = gitUrl;
        this.projectName = name;
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        if (gitUrl.trim().isEmpty() || projectName.trim().isEmpty()) {
            Services.getInstance(project, Notifier.class).error(project, "Clone Error", "Missing parameters for cloning the project.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Cloning repository", false) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Cloning into " + projectName + "...");

                try {
                    GitCommandRunner.execute(Services.getInstance(project, Setting.class).getTestinPath(), "git", "clone", gitUrl, projectName);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile vRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(Services.getInstance(project, Setting.class).getTestinPath().toFile());
                        if (vRoot != null) {
                            vRoot.refresh(false, true);
                        }

                        final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);
                        final Path projectPath = Services.getInstance(project, Setting.class).getTestinPath().resolve(projectName);

                        indexer.scanSingleProject(projectPath);
                        final TestProjectDirectoryDto clonedProject = indexer.getTestProjectsByPath().get(projectPath.toString());
                        if (clonedProject != null)
                            projectPanel.getTestProjectSelector().addTestProject(clonedProject);

                        projectPanel.getProjectTree().updateNodes();
                        Services.getInstance(project, Notifier.class).info(project, "Clone Successful", "Project '" + projectName + "' was cloned successfully.");
                    });

                } catch (final Exception ex) {
                    Services.getInstance(project, Notifier.class).error(project, "Clone Failed", "Could not clone repository:\n" + ex.getMessage());
                }
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}