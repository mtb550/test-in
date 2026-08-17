package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.PackageStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.PackageMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

/**
 * Archives a package, or brings it back, from the tree context menu - one
 * instance per {@link PackageStatus}. A test set package and a test run package
 * are the same thing to this action: whichever node is selected, its marker is
 * a {@link PackageMarker}, and that is all it asks for. Shown only on a package,
 * and enabled only when it would change something.
 */
public class UpdatePackageStatusAction extends AbstractProjectTreeAction {
    private final @NotNull PackageStatus status;

    public UpdatePackageStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull PackageStatus status) {
        super(p, tree, status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final DirectoryDto dir = TreeValueUtil.singleSelectedDirectory(tree);
        final PackageMarker marker = markerOf(dir);
        if (dir == null || marker == null) return;

        try {
            marker.setStatus(status);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(dir);

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().refresh();

            // The status names itself: "Archived", "Active" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to mark package '" + dir.getName() + "' " + status.getLabel() + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to mark package " + status.getLabel());
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final PackageMarker marker = markerOf(TreeValueUtil.singleSelectedDirectory(tree));

        e.getPresentation().setVisible(marker != null);
        e.getPresentation().setEnabled(marker != null && marker.getStatus() != status);
    }

    private static @Nullable PackageMarker markerOf(final @Nullable DirectoryDto dir) {
        return dir != null && dir.getMarker() instanceof PackageMarker marker ? marker : null;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
