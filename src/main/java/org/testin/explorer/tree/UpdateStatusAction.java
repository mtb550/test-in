package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.NodeStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.Marker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;

import java.util.Optional;

/**
 * Sets one status on the selected node - one instance per status the node
 * offers, built by {@link UpdateStatusGroup}.
 * <p>
 * Whose status it is, this never asks. A test project, a test set and a package
 * each carry a marker, the marker says which statuses it accepts and applies
 * the one it is given, and setting any of them is the same four steps: mark it,
 * stamp who did it, write the marker, redraw. There were three of these, one
 * per kind, differing in the name of a DTO class and in nothing else, so a fix
 * to one - a missing {@code touch}, a notification that never appeared - was a
 * fix to one of three (#110).
 * <p>
 * Which status it sets is the only thing it carries; the project and the tree
 * come from the keystroke.
 */
public class UpdateStatusAction extends DumbAwareAction {
    private final @NotNull NodeStatus status;

    public UpdateStatusAction(final @NotNull NodeStatus status) {
        super(status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        selected(e).ifPresent(dir -> mark(p, dir));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull Marker marker = dir.getMarker();
        try {
            marker.applyStatus(status);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(dir);

            // The panel, not only the tree: a test project going inactive
            // changes what the panel has to show, and a package going archived
            // changes where it sits in the tree the panel then draws. One call
            // covers both, and it is the one removing a node already makes.
            Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();

            // The status names itself: "Archived", "Deprecated", "Active" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to mark '" + dir.getName() + "' " + status.getLabel() + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("status.failed", status.getLabel()));
        }
    }

    /**
     * The node selected on its own, when this status is one of its own - empty
     * on any other node, which is what takes the entry off the menu.
     */
    private @NotNull Optional<DirectoryDto> selected(final @NotNull AnActionEvent e) {
        return TestinData.singleSelectedNode(e).filter(dir -> dir.getMarker().statuses().contains(status));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<DirectoryDto> dir = selected(e);

        e.getPresentation().setVisible(dir.isPresent());
        e.getPresentation().setEnabled(dir.filter(node -> node.getMarker().status() != status).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
