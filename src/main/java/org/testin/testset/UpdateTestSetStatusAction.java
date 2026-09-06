package org.testin.testset;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestSetStatus;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

/**
 * Sets a test set's status from the tree context menu - one instance per
 * {@link TestSetStatus}, the way the test project's status actions are built.
 * Shown only on a test set, and enabled only when it would change something.
 */
public class UpdateTestSetStatusAction extends AbstractProjectTreeAction {
    private final @NotNull TestSetStatus status;

    public UpdateTestSetStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree, final @NotNull TestSetStatus status) {
        super(p, tree, status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        selectedTestSet().ifPresent(this::mark);
    }

    private void mark(final @NotNull TestSetDirectoryDto ts) {
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

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<TestSetDirectoryDto> ts = selectedTestSet();

        e.getPresentation().setVisible(ts.isPresent());
        e.getPresentation().setEnabled(ts.filter(set -> set.getMarker().getStatus() != status).isPresent());
    }

    private @NotNull Optional<TestSetDirectoryDto> selectedTestSet() {
        return TreeValueUtil.singleSelected(tree, TestSetDirectoryDto.class);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
