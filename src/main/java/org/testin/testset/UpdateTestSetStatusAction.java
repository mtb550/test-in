package org.testin.testset;

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
import org.testin.model.TestSetStatus;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

/**
 * Sets a test set's status - one instance per {@link TestSetStatus}, the way the
 * test project's status actions are built. Shown only on a test set, and enabled
 * only when it would change something.
 * <p>
 * Built by {@link UpdateTestSetStatusGroup}, which is what {@code plugin.xml}
 * declares (#119): a status is a constant on an enum, so the entries are
 * generated from {@code values()} rather than written out one XML element each.
 * Which status it sets is the only thing it carries - the project and the tree
 * come from the keystroke.
 */
public class UpdateTestSetStatusAction extends DumbAwareAction {
    private final @NotNull TestSetStatus status;

    public UpdateTestSetStatusAction(final @NotNull TestSetStatus status) {
        super(status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        selectedTestSet(e).ifPresent(ts -> mark(p, ts));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull TestSetDirectoryDto ts) {
        try {
            ts.getMarker().setStatus(status);
            ts.getMarker().touch(Services.getInstance(p, AppSettingsState.class).testerName);
            Services.getInstance(p, ProjectIndexer.class).persistMarker(ts);

            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

            // The status names itself: "Deprecated", "Active" (#62).
            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to mark test set '" + ts.getName() + "' " + status.getLabel() + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, "Unable to mark test set " + status.getLabel());
        }
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<TestSetDirectoryDto> ts = selectedTestSet(e);

        e.getPresentation().setVisible(ts.isPresent());
        e.getPresentation().setEnabled(ts.filter(set -> set.getMarker().getStatus() != status).isPresent());
    }

    private @NotNull Optional<TestSetDirectoryDto> selectedTestSet(final @NotNull AnActionEvent e) {
        return TestinData.singleSelected(e, TestSetDirectoryDto.class);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
