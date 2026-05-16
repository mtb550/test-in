package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.git.GitCommandRunner;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.nio.file.Path;

public class CreateBranch extends DumbAwareAction {

    private final SimpleTree tree;

    public CreateBranch(SimpleTree tree) {
        super("Create New Branch", "Create a new Git branch for your test cases", AllIcons.Vcs.Branch);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Notifier.getInstance().error("Branch Error", "Could not determine the active project.");
            return;
        }

        File gitDir = new File(repoPath.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            Notifier.getInstance().warn("Branch Error", "This project is not a Git repository.");
            return;
        }

        String branchName = Messages.showInputDialog(
                Config.getProject(),
                "Enter a name for the new branch (e.g., feature/login-tests):",
                "Create New Branch",
                AllIcons.Vcs.Branch,
                "",
                null
        );

        if (branchName == null || branchName.trim().isEmpty()) return;

        // Sanitize branch name (replace spaces with hyphens)
        final String safeBranchName = branchName.trim().replaceAll("\\s+", "-");

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Creating branch", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    // Create and checkout the new branch locally
                    GitCommandRunner.execute(repoPath, "git", "checkout", "-b", safeBranchName);

                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().info("Branch Created", "Switched to new branch: " + safeBranchName)
                    );

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Branch Failed", "Could not create branch:\n" + ex.getMessage())
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
        e.getPresentation().setEnabled(tree.getModel().getRoot() != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}