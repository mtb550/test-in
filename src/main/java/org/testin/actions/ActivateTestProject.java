package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.markers.TestProjectMarker;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.ZonedDateTime;

public class ActivateTestProject extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    public ActivateTestProject(final @NotNull SimpleTree tree) {
        super("Activate", "Activate test project", AllIcons.Actions.Edit);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof TestProjectDirectoryDto tp)) return;

        final Project project = e.getProject();
        if (project == null) return;

        try {
            final TestProjectMarker marker = tp.getMarker();

            marker.setStatus(ProjectStatus.ACTIVE);
            marker.setUpdatedBy(System.getProperty("user.name", ""));
            marker.setUpdatedAt(ZonedDateTime.now());
            tp.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).persistTestProjectMarker(project, tp);

            tree.revalidate();
            tree.repaint();

            Services.getInstance(project, Notifier.class).info(project, "Test project '" + tp.getName() + "' has been activated.");

        } catch (Exception ex) {
            Log.error("Failed to activate project: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "Activation Failed", "Could not activate test project.");
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        e.getPresentation().setEnabled(node.getUserObject() instanceof TestProjectDirectoryDto);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
