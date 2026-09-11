package org.testin.testrun;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.tree.TreeValues;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;


public class SetTestRunStatusAction extends AbstractProjectAction {
    final @NotNull SimpleTree tree;

    public SetTestRunStatusAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, Bundle.message("run.set.status.text"), Bundle.message("run.set.status.description"), AllIcons.Nodes.Test);
        this.tree = tree;
    }

    // UC-TREE-PANEL-020
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeValues.selected(tree, TestRunDirectoryDto.class).ifPresent(this::askForStatus);
    }

    /**
     * Inside the menu callback, so a dismissed menu changes nothing and says
     * nothing (#62).
     */
    private void askForStatus(final @NotNull TestRunDirectoryDto testRunDto) {
        new TestRunStatusMenuDialog(p, testRunDto.getMarker().getStatus(), selectedStatus ->
                Services.getInstance(p, TestRunStatusChange.class).apply(testRunDto, selectedStatus)).show();
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-067
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValues.selected(tree, TestRunDirectoryDto.class)
                .filter(TestRunDirectoryDto::isStillOpen)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
