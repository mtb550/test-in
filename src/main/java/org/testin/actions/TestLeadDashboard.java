package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.util.git.GitCommandRunner;
import org.testin.util.git.ReviewBranchesDialog;
import org.testin.util.notifications.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TestLeadDashboard extends DumbAwareAction {

    private final SimpleTree tree;

    public TestLeadDashboard(SimpleTree tree) {
        super("Test Lead Dashboard", "Review and merge incoming test case branches", AllIcons.Actions.InlayGear);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Notifier.getInstance().error("Error", "Could not determine the active project.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Fetching branches", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    // Fetch latest data from GitHub
                    GitCommandRunner.execute(repoPath, "git", "fetch", "--prune");

                    // Get a list of all remote branches
                    String branchesOutput = GitCommandRunner.execute(repoPath, "git", "branch", "-r");
                    List<String> branches = Arrays.asList(branchesOutput.split("\n"));

                    ApplicationManager.getApplication().invokeLater(() -> {
                        ReviewBranchesDialog dialog = new ReviewBranchesDialog(Config.getProject(), repoPath, branches);
                        dialog.show();
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Notifier.getInstance().error("Fetch Failed", "Could not retrieve branches from GitHub:\n" + ex.getMessage())
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