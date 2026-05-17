package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.util.git.GitCommandRunner;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;

public class CloneProject extends DumbAwareAction {

    private final String gitUrl;
    private final String projectName;
    private final Path targetPath;

    public CloneProject(String gitUrl, String projectName, Path targetPath) {
        super("Clone Git Project", "Import an existing test project from Git", AllIcons.Vcs.Clone);
        this.gitUrl = gitUrl;
        this.projectName = projectName;
        this.targetPath = targetPath;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        execute();
    }

    public void execute() {
        if (targetPath == null) {
            Notifier.getInstance().error("Setup Error", "Root TestZoom folder is not set. Please configure it in settings first.");
            return;
        }

        if (gitUrl == null || gitUrl.trim().isEmpty() || projectName == null || projectName.trim().isEmpty()) {
            Notifier.getInstance().error("Clone Error", "Missing parameters for cloning the project.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Cloning repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Cloning into " + projectName + "...");

                try {
                    GitCommandRunner.execute(targetPath, "git", "clone", gitUrl, projectName);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile vRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetPath.toFile());
                        if (vRoot != null) {
                            vRoot.refresh(false, true);
                        }
                        Notifier.getInstance().info("Clone Successful", "Project '" + projectName + "' was cloned successfully.");
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Clone Failed", "Could not clone repository:\n" + ex.getMessage())
                    );
                }
            }
        });
    }
}