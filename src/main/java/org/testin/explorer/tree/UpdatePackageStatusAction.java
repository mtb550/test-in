package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.PackageStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.PackageMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

/**
 * Archives a package, or brings it back - one instance per {@link
 * PackageStatus}. A test set package and a test run package are the same thing
 * to this action: whichever node is selected, its marker is a {@link
 * PackageMarker}, and that is all it asks for. Shown only on a package, and
 * enabled only when it would change something.
 * <p>
 * Built by {@link UpdatePackageStatusGroup}, which is what {@code plugin.xml}
 * declares (#119): a status is a constant on an enum, so the entries are
 * generated from {@code values()} rather than written out one XML element each.
 * Which status it sets is the only thing it carries - the project and the tree
 * come from the keystroke.
 */
public class UpdatePackageStatusAction extends DumbAwareAction {
    private final @NotNull PackageStatus status;

    public UpdatePackageStatusAction(final @NotNull PackageStatus status) {
        super(status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    /**
     * The package marker of the node selected on its own - empty on anything
     * that is not a package, which is what greys the entry out.
     */
    private @NotNull Optional<PackageMarker> selectedMarker(final @NotNull AnActionEvent e) {
        return TestinData.singleSelectedNode(e)
                .map(DirectoryDto::getMarker)
                .filter(PackageMarker.class::isInstance)
                .map(PackageMarker.class::cast);
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.singleSelectedNode(e)
                .filter(dir -> dir.getMarker() instanceof PackageMarker)
                .ifPresent(dir -> mark(p, dir));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull PackageMarker marker = (PackageMarker) dir.getMarker();
        try {
            marker.setStatus(status);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(dir);

            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

            // The status names itself: "Archived", "Active" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to mark package '" + dir.getName() + "' " + status.getLabel() + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to mark package " + status.getLabel());
        }
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<PackageMarker> marker = selectedMarker(e);

        e.getPresentation().setVisible(marker.isPresent());
        e.getPresentation().setEnabled(marker.filter(m -> m.getStatus() != status).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
