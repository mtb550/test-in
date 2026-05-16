package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.util.git.GitCommandRunner;
import org.testin.util.notifications.Notifier;

import java.nio.file.Path;

public class CloneProject extends DumbAwareAction {

    public CloneProject() {
        super("Clone Git Project", "Import an existing test project from Git", AllIcons.Vcs.Clone);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Path rootTestinPath = Config.getTestinPath();

        if (rootTestinPath == null) {
            Notifier.getInstance().error("Setup Error", "Root Testin folder is not set. Please configure it in settings first.");
            return;
        }

        String gitUrl = Messages.showInputDialog(
                Config.getProject(),
                "Enter the Git repository URL (e.g., https://github.com/org/repo.git):",
                "Clone Test Project",
                AllIcons.Vcs.Clone,
                "",
                null
        );

        if (gitUrl == null || gitUrl.trim().isEmpty()) return;

        String projectName = Messages.showInputDialog(
                Config.getProject(),
                "Enter a folder name for this project.\n",
                "Project Name",
                AllIcons.Nodes.Folder,
                "NewProject",
                null
        );

        if (projectName == null || projectName.trim().isEmpty()) return;

        final String finalProjectName = projectName.trim();

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Cloning repository", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Cloning into " + finalProjectName + "...");

                try {
                    GitCommandRunner.execute(rootTestinPath, "git", "clone", gitUrl.trim(), finalProjectName);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        VirtualFile vRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(rootTestinPath.toFile());
                        if (vRoot != null) {
                            vRoot.refresh(false, true);
                        }
                        Notifier.getInstance().info("Clone Successful", "Project '" + finalProjectName + "' was cloned successfully.");
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