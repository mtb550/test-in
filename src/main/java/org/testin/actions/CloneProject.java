package org.testin.actions;

import com.intellij.icons.AllIcons;
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
import org.testin.pojo.Config;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.GitCommandRunner;
import org.testin.util.notifications.Notifier;

public class CloneProject extends DumbAwareAction {

    private final String gitUrl;
    private final String projectName;
    private final ProjectPanel projectPanel;

    public CloneProject(final String gitUrl, final String projectName, final ProjectPanel projectPanel) {
        super("Clone Git Project", "Import an existing test project from Git", AllIcons.Vcs.Clone);
        this.gitUrl = gitUrl;
        this.projectName = projectName;
        this.projectPanel = projectPanel;
    }

    private Project project;

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        execute(e.getProject());
    }

    public void execute(final Project project) {
        this.project = project;

        if (gitUrl == null || gitUrl.trim().isEmpty() || projectName == null || projectName.trim().isEmpty()) {
            Notifier.getInstance().error(project, "Clone Error", "Missing parameters for cloning the project.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Cloning repository", false) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Cloning into " + projectName + "...");

                try {
                    GitCommandRunner.execute(Config.getTestinPath(), "git", "clone", gitUrl, projectName);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile vRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(Config.getTestinPath().toFile());
                        if (vRoot != null) {
                            vRoot.refresh(false, true);
                        }
                        Notifier.getInstance().info(project, "Clone Successful", "Project '" + projectName + "' was cloned successfully.");
                        new Refresh(projectPanel).execute();
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error(project, "Clone Failed", "Could not clone repository:\n" + ex.getMessage())
                    );
                }
            }
        });
    }
}