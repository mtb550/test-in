package testGit.projectPanel.tree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.actions.Open;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseAdapterImpl extends MouseAdapter {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;
    private final ContextMenu contextMenu;

    public MouseAdapterImpl(ProjectPanel projectPanel, SimpleTree tree, ContextMenu contextMenu) {
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.contextMenu = contextMenu;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());

        if (selPath == null || !(selPath.getLastPathComponent() instanceof DefaultMutableTreeNode node) || !(node.getUserObject() instanceof Directory))
            return;

        if (SwingUtilities.isRightMouseButton(e)) {
            ActionPopupMenu popupMenu = ActionManager.
                    getInstance()
                    .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu);
            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            Open.execute(projectPanel, tree);
            e.consume();
        }
    }
}