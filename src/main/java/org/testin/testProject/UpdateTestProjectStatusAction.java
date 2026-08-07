package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.markers.TestProjectMarker;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.ZonedDateTime;

public class UpdateTestProjectStatusAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull ProjectStatus projectStatus;

    public UpdateTestProjectStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ProjectStatus projectStatus) {
        super(projectStatus.getButtonName(), projectStatus.getButtonDescription(), AllIcons.Actions.Edit);
        this.p = p;
        this.tree = tree;
        this.projectStatus = projectStatus;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        final Object userObject = node.getUserObject();
        if (!(userObject instanceof TestProjectDirectoryDto tp)) return;

        try {
            final TestProjectMarker marker = tp.getMarker();
            marker.setStatus(projectStatus);
            marker.setUpdatedBy(System.getProperty("user.name", ""));
            marker.setUpdatedAt(ZonedDateTime.now());
            tp.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).persistTestProjectMarker(p, tp);

            Services.getInstance(p, ProjectPanel.class).getProjectTree().updateNodes();

            Services.getInstance(p, Notifier.class).info(p, "Test project '" + tp.getName() + "' is " + projectStatus.getDescription() + ".");

        } catch (final Exception ex) {
            Logger.error("Unable to update status to " + projectStatus.getDescription());
            Logger.error(ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to update status to " + projectStatus.getDescription());
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
