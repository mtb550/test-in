package org.testin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.view.marker.MarkerDetailsViewDialog;


public class ShowNodeDetailsAction extends AbstractProjectTreeAction {

    public ShowNodeDetailsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Details", "Show node details", AllIcons.General.IndentDetected);
    }

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-087
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreeValueUtil.singleSelectedDirectory(tree).ifPresent(dir -> new MarkerDetailsViewDialog(p, dir).show());
    }

    /**
     * UC-TREE-PANEL-027, Rule-TREE-PANEL-087.
     * <p>
     * The dialog is about one node, so several selected grays the entry. It had
     * no update() at all: the entry stayed black over any selection and opened
     * the first row's details, with nothing to say the rest had been passed over
     * (#192).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.singleSelectedDirectory(tree).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT now: update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }

}
