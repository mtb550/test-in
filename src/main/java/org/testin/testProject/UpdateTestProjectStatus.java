package org.testin.testProject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.markers.TestProjectMarker;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.time.ZonedDateTime;

public class UpdateTestProjectStatus extends DumbAwareAction {

    private final @NotNull SimpleTree tree;
    private final @NotNull ProjectStatus projectStatus;

    public UpdateTestProjectStatus(final @NotNull SimpleTree tree, final @NotNull ProjectStatus projectStatus) {
        super(projectStatus.getButtonName(), projectStatus.getButtonDescription(), AllIcons.Actions.Edit);
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

        final Project project = e.getProject();
        if (project == null) return;

        try {
            final TestProjectMarker marker = tp.getMarker();
            marker.setStatus(projectStatus);
            marker.setUpdatedBy(System.getProperty("user.name", ""));
            marker.setUpdatedAt(ZonedDateTime.now());
            tp.setMarker(marker);

            Services.getInstance(project, ProjectIndexer.class).persistTestProjectMarker(project, tp);

            Services.getInstance(project, ProjectPanel.class).getProjectTree().updateNodes();

            Services.getInstance(project, Notifier.class).info(project, "Test project '" + tp.getName() + "' is " + projectStatus.getDescription() + ".");

        } catch (final Exception ex) {
            Logger.error("Unable to update status to " + projectStatus.getDescription());
            Logger.error(ex.getMessage());
            Services.getInstance(project, Notifier.class).error(project, "Unable to update status to " + projectStatus.getDescription());
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
