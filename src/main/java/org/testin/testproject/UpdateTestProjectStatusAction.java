package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.markers.TestProjectMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

/**
 * Sets a test project's status from the tree context menu - one instance per
 * {@link ProjectStatus}. Shown only on a test project, and enabled only when it
 * would change something, the way its test set and package siblings are.
 */
public class UpdateTestProjectStatusAction extends AbstractProjectTreeAction {
    private final @NotNull ProjectStatus projectStatus;

    public UpdateTestProjectStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull ProjectStatus projectStatus) {
        super(p, tree, projectStatus.getButtonName(), projectStatus.getButtonDescription(), AllIcons.Actions.Edit);
        this.projectStatus = projectStatus;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final TestProjectDirectoryDto tp = selectedTestProject();
        if (tp == null) return;

        try {
            final TestProjectMarker marker = tp.getMarker();
            marker.setStatus(projectStatus);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            tp.setMarker(marker);

            Services.getInstance(p, ProjectIndexer.class).persistTestProjectMarker(p, tp);

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().updateNodes();

            // The status names itself: "Active", "Inactive", "Archived" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, projectStatus.getDescription());

        } catch (final Exception ex) {
            Logger.error("Unable to update status to " + projectStatus.getDescription());
            Logger.error(ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to update status to " + projectStatus.getDescription());
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TestProjectDirectoryDto tp = selectedTestProject();

        e.getPresentation().setVisible(tp != null);
        e.getPresentation().setEnabled(tp != null && tp.getMarker().getStatus() != projectStatus);
    }

    private @Nullable TestProjectDirectoryDto selectedTestProject() {
        return TreeValueUtil.singleSelectedDirectory(tree) instanceof TestProjectDirectoryDto tp ? tp : null;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
