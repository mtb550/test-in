package org.testin.actions;

import com.intellij.icons.AllIcons;
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

public class Activate extends DumbAwareAction {
    private final SimpleTree tree;

    public Activate(final SimpleTree tree) {
        super("Activate", "", AllIcons.Actions.Edit);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (!(userObject instanceof TestProjectDirectoryDto tp)) return;

        Project project = e.getProject();
        if (project == null) return;

        try {
            TestProjectMarker marker = tp.getMarker();
            if (marker == null) {
                marker = TestProjectMarker.builder()
                        .status(ProjectStatus.ACTIVE)
                        .createdBy(System.getProperty("user.name", ""))
                        .build();
            } else {
                marker.setStatus(ProjectStatus.ACTIVE);
            }

            tp.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).updateProjectMarker(project, tp.getPath(), marker);

            tree.revalidate();
            tree.repaint();

            Services.getInstance(project, Notifier.class).info(project, "Activate", "Test project '" + tp.getName() + "' has been activated.");
        } catch (Exception ex) {
            Log.error("Failed to activate project: " + ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "Activation Failed", "Could not activate test project.");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        e.getPresentation().setEnabled(userObject instanceof TestProjectDirectoryDto);
    }
}
