package org.testin.testproject;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.ExplorerPanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
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
        selectedTestProject().ifPresent(this::mark);
    }

    private void mark(final @NotNull TestProjectDirectoryDto tp) {
        try {
            tp.getMarker().setStatus(projectStatus);
            tp.getMarker().touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(tp);

            Services.getInstance(p, ExplorerPanel.class).getProjectTree().updateNodes();

            // The status names itself: "Active", "Inactive", "Archived" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, projectStatus.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to update status to " + projectStatus.getLabel());
            Logger.error(ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to update status to " + projectStatus.getLabel());
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<TestProjectDirectoryDto> tp = selectedTestProject();

        e.getPresentation().setVisible(tp.isPresent());
        e.getPresentation().setEnabled(tp.filter(project -> project.getMarker().getStatus() != projectStatus).isPresent());
    }

    private @NotNull Optional<TestProjectDirectoryDto> selectedTestProject() {
        return TreeValueUtil.singleSelected(tree, TestProjectDirectoryDto.class);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
