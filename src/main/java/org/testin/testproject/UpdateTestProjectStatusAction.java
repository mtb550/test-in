package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.enums.ProjectStatus;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.markers.TestProjectMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import javax.swing.tree.TreePath;

public class UpdateTestProjectStatusAction extends AbstractProjectTreeAction {
    private final @NotNull ProjectStatus projectStatus;

    public UpdateTestProjectStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ProjectStatus projectStatus) {
        super(p, tree, projectStatus.getButtonName(), projectStatus.getButtonDescription(), AllIcons.Actions.Edit);
        this.projectStatus = projectStatus;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final TestProjectDirectoryDto tp = TreeValueUtil.valueOf(path.getLastPathComponent(), TestProjectDirectoryDto.class);
        if (tp == null) return;

        try {
            final TestProjectMarker marker = tp.getMarker();
            marker.setStatus(projectStatus);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            tp.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).persistTestProjectMarker(p, tp);

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().updateNodes();

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
        e.getPresentation().setEnabled(TreeValueUtil.valueOf(path.getLastPathComponent(), TestProjectDirectoryDto.class) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
