package org.testin.testRun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.TestRunStatusMenuDialog;
import org.testin.enums.TestRunStatus;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.markers.TestRunMarker;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

// todo, to be refactored
public class SetTestRunStatusAction extends DumbAwareAction {
    final @NotNull SimpleTree tree;

    public SetTestRunStatusAction(final @NotNull SimpleTree tree) {
        super("Set Status", "Set test run status", AllIcons.Nodes.Test);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();

        if (userObject instanceof TestRunDirectoryDto testRunDto) {
            new TestRunStatusMenuDialog(project, selectedStatus -> {
                TestRunMarker marker = testRunDto.getMarker();
                marker.setStatus(selectedStatus);
                marker.setCreatedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                Logger.trace("Status changed -> " + testRunDto.getName() + " = " + selectedStatus.getLabel());

                persistMarker(project, testRunDto, selectedStatus);

                tree.repaint();
            }).show();
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = parentNode.getUserObject();
        boolean enabled = userObject instanceof TestRunDirectoryDto dir &&
                dir.getMarker() != null &&
                dir.getMarker().getStatus() != TestRunStatus.COMPLETED &&
                dir.getMarker().getStatus() != TestRunStatus.CLOSED;

        e.getPresentation().setEnabled(enabled);
    }

    private void persistMarker(final @NotNull Project p, final TestRunDirectoryDto tr, final TestRunStatus newStatus) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final TestRunDirectoryDto trd = indexer.getTestRunDirByPath(tr.getPath());

                TestRunMarker marker = trd.getMarker();
                if (marker != null) {
                    marker.setStatus(newStatus);

                    indexer.updateRunMarker(p, tr.getPath(), marker);
                }
            } catch (final Exception ex) {
                Logger.error("Failed to persist marker: " + ex.getMessage());
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
