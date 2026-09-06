package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;


public class SetTestRunStatusAction extends AbstractProjectAction {
    final @NotNull SimpleTree tree;

    public SetTestRunStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Set Status", "Set test run status", AllIcons.Nodes.Test);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeValueUtil.selected(tree, TestRunDirectoryDto.class).ifPresent(this::askForStatus);
    }

    private void askForStatus(final @NotNull TestRunDirectoryDto testRunDto) {
        new TestRunStatusMenuDialog(p, selectedStatus -> {
            Logger.trace("Status changed -> " + testRunDto.getName() + " = " + selectedStatus.getLabel());

            // Updates the indexer-owned marker (single source of truth) and
            // persists it through the sequential run-status writer.
            Services.getInstance(p, RunStatusService.class).persistMarker(
                    p, testRunDto.getPath(), selectedStatus);

            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

            // The status names itself, as verdicts do: "Completed", "Closed".
            // Inside the menu callback, so a dismissed menu says nothing (#62).
            Services.getInstance(p, Notifier.class).softShow(p, selectedStatus.getLabel());
        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.selected(tree, TestRunDirectoryDto.class)
                .filter(TestRunDirectoryDto::isStillOpen)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
