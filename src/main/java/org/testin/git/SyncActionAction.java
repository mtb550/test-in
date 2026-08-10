package org.testin.git;

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
import git4idea.GitUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;

import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class SyncActionAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull ProjectPanel pp;
    private final @NotNull GitRepositoryService git;
    private final @NotNull GitSyncService sync;

    public SyncActionAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ProjectPanel pp) {
        super("Sync / Pull Changes", "Pull the latest test cases from the remote repository", AllIcons.Actions.SyncPanels);
        this.p = p;
        this.tree = tree;
        this.pp = pp;
        this.git = new GitRepositoryService(p);
        this.sync = new GitSyncService(p);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        Path repoPath = getActiveProjectPath();

        if (repoPath == null) {
            Services.getInstance(p, Notifier.class).error(p, "Sync Error", "Could not determine the active project. Please select a project in the tree.");
            return;
        }

        if (!git.isRepository(repoPath)) {
            Services.getInstance(p, Notifier.class).warn(p, "Sync Error", "This project is not a Git repository. Initialize it first.");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(p, "Syncing with remote", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    indicator.setText("Checking remote configuration...");
                    String remoteUrl = git.getRemoteUrl(repoPath);

                    if (remoteUrl.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Services.getInstance(p, Notifier.class).warn(p, "Sync Aborted", "No remote URL is configured for this project. Push a commit first to configure the remote.")
                        );
                        return;
                    }

                    indicator.setText("Pulling latest changes...");
                    sync.pullMain(repoPath);

                    indicator.setText("Refreshing files...");
                    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(repoPath.toFile());
                    if (vFile != null) {
                        GitUtil.refreshVfsInRoot(vFile);
                    }
                    Services.getInstance(p, ProjectIndexer.class).scanSingleProject(repoPath);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        Services.getInstance(p, Notifier.class).info(p, "Sync Successful", "Your project is now up to date with the remote repository.");
                        pp.getProjectTree().refresh();
                    });

                } catch (final Exception ex) {
                    Logger.error(ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() -> Services.getInstance(p, Notifier.class).error(p, "Sync Failed", "Could not pull changes:\n" + ex.getMessage())
                    );
                }
            }
        });
    }

    private Path getActiveProjectPath() {
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null) {
            for (Object component : selectionPath.getPath()) {
                TestProjectDirectoryDto project = TreeValueUtil.valueOf(component, TestProjectDirectoryDto.class);
                if (project != null) return project.getPath();
            }
        }

        TestProjectDirectoryDto prj = TreeValueUtil.valueOf(tree.getModel().getRoot(), TestProjectDirectoryDto.class);
        if (prj != null) {
            return prj.getPath();
        }
        return null;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;
        e.getPresentation().setEnabled(TreeValueUtil.valueOf(path.getLastPathComponent(), TestProjectDirectoryDto.class) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
