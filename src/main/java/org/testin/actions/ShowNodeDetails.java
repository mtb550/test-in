package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.viewPanel.markerDetails.MarkerDetailsView;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ShowNodeDetails extends DumbAwareAction {
    private final SimpleTree tree;

    public ShowNodeDetails(final @NotNull SimpleTree tree) {
        super("Details", "Show node details", AllIcons.General.IndentDetected);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = treeNode.getUserObject();

        if (!(userObject instanceof DirectoryDto dir)) return;

        MarkerDetailsView.show(e.getProject(), dir);
    }
}
