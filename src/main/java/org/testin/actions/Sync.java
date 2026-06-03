package org.testin.actions;

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
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.GitCommandRunner;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;

public class Sync extends DumbAwareAction {

    private final SimpleTree tree;
    private final ProjectPanel projectPanel;

    public Sync(final SimpleTree tree, final ProjectPanel projectPanel) {
        super("Sync / Pull Changes", "Pull the latest test cases from the remote repository", AllIcons.Actions.SyncPanels);
        this.tree = tree;
        this.projectPanel = projectPanel;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Notifier.getInstance().error(project, "Sync Error", "Could not determine the active project. Please select a project in the tree.");
            return;
        }

        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            Notifier.getInstance().warn(project, "Sync Error", "This project is not a Git repository. Initialize it first.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Syncing with remote", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    indicator.setText("Checking remote configuration...");
                    String remoteUrl = "";
                    try {
                        remoteUrl = GitCommandRunner.execute(repoPath, "git", "config", "--get", "remote.origin.url").trim();
                    } catch (Exception ignored) {
                    }

                    if (remoteUrl.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Notifier.getInstance().warn(project, "Sync Aborted", "No remote URL is configured for this project. Push a commit first to configure the remote.")
                        );
                        return;
                    }

                    indicator.setText("Pulling latest changes...");
                    GitCommandRunner.execute(repoPath, "git", "pull", "--rebase", "--autostash", "origin", "main");

                    indicator.setText("Refreshing files...");
                    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile());
                    if (vFile != null) {
                        vFile.refresh(false, true);
                    }

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Notifier.getInstance().info(project, "Sync Successful", "Your project is now up to date with the remote repository.");
                        if (projectPanel != null) {
                            projectPanel.setupMainLayout();
                        }
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error(project, "Sync Failed", "Could not pull changes:\n" + ex.getMessage())
                    );
                }
            }
        });
    }

    private Path getActiveProjectPath() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            while (node != null) {
                if (node.getUserObject() instanceof TestProjectDirectoryDto prj) {
                    return prj.getPath();
                }
                node = (DefaultMutableTreeNode) node.getParent();
            }
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root != null && root.getUserObject() instanceof TestProjectDirectoryDto prj) {
            return prj.getPath();
        }
        return null;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;
        final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = selectedNode.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestProjectDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}