package org.testin.projectPanel.tree;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.open.OpenAction;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TreeMouseListener extends PopupHandler {
    private final @NotNull Project p;
    private final SimpleTree tree;
    private final TreeContextMenu treeContextMenu;

    public TreeMouseListener(final @NotNull Project p, final SimpleTree tree, final TreeContextMenu treeContextMenu) {
        this.p = p;
        this.tree = tree;
        this.treeContextMenu = treeContextMenu;
    }

    @Override
    public void invokePopup(final Component comp, final int x, final int y) {
        TreePath selPath = tree.getPathForLocation(x, y);

        if (selPath != null && selPath.getLastPathComponent() instanceof DefaultMutableTreeNode node) {
            if (node.getUserObject() instanceof DirectoryDto) {

                if (!tree.getSelectionModel().isPathSelected(selPath)) {
                    tree.setSelectionPath(selPath);
                }

                ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, treeContextMenu);
                popupMenu.getComponent().show(comp, x, y);
            }
        }
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());

        if (selPath == null || !(selPath.getLastPathComponent() instanceof DefaultMutableTreeNode node) || !(node.getUserObject() instanceof DirectoryDto))
            return;

        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            new OpenAction(p, tree).execute(p);
            e.consume();
        }
    }
}