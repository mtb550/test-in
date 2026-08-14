package org.testin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.viewPanel.markerDetails.MarkerDetailsViewDialog;

import javax.swing.tree.TreePath;

public class ShowNodeDetailsAction extends AbstractProjectAction {
    private final @NotNull SimpleTree tree;

    public ShowNodeDetailsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, "Details", "Show node details", AllIcons.General.IndentDetected);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        if (!(userObject instanceof DirectoryDto dir)) return;

        new MarkerDetailsViewDialog(p).show(dir);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - no update() here reads Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }

}
