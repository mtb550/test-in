package org.testin.testproject;

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
import org.testin.model.ProjectStatus;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;

/**
 * Sets a test project's status - one instance per {@link ProjectStatus}. Shown
 * only on a test project, and enabled only when it would change something, the
 * way its test set and package siblings are.
 * <p>
 * Built by {@link UpdateTestProjectStatusGroup}, which is what {@code
 * plugin.xml} declares (#119). A status is a constant on an enum, so the entries
 * are generated from {@code values()} rather than written out one XML element
 * each - which is the rule that keeps a status added there from being one
 * nothing can set (#175, C9).
 * <p>
 * Which status it sets is the only thing it carries. The project and the tree
 * are gone: what it acts on comes from the keystroke.
 */
public class UpdateTestProjectStatusAction extends DumbAwareAction {
    private final @NotNull ProjectStatus projectStatus;

    public UpdateTestProjectStatusAction(final @NotNull ProjectStatus projectStatus) {
        super(projectStatus.getButtonName(), projectStatus.getButtonDescription(), AllIcons.Actions.Edit);
        this.projectStatus = projectStatus;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        selectedTestProject(e).ifPresent(tp -> mark(p, tp));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull TestProjectDirectoryDto tp) {
        try {
            tp.getMarker().setStatus(projectStatus);
            tp.getMarker().touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(tp);

            Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();

            // The status names itself: "Active", "Inactive", "Archived" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, projectStatus.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to update status to " + projectStatus.getLabel());
            Logger.error(ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("project.status.failed", projectStatus.getLabel()));
        }
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<TestProjectDirectoryDto> tp = selectedTestProject(e);

        e.getPresentation().setVisible(tp.isPresent());
        e.getPresentation().setEnabled(tp.filter(project -> project.getMarker().getStatus() != projectStatus).isPresent());
    }

    private @NotNull Optional<TestProjectDirectoryDto> selectedTestProject(final @NotNull AnActionEvent e) {
        return TestinData.singleSelected(e, TestProjectDirectoryDto.class);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
