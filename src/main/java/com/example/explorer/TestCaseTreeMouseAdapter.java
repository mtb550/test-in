package com.example.explorer;

import com.example.editor.TestCaseEditor;
import com.example.pojo.Tree;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestCaseTreeMouseAdapter extends MouseAdapter {
    private final JTree tree;

    public TestCaseTreeMouseAdapter(JTree tree) {
        this.tree = tree;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) return;

        Object userObject = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();

        if (!(userObject instanceof Tree treeItem)) return;

        if (SwingUtilities.isRightMouseButton(e)) {
            ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("TestTreeContextMenuGroup");
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group);
            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

        } else if (e.getClickCount() == 2 && treeItem.getType() == 2) {
            System.out.println(((Tree) userObject).getName());
            System.out.println(((Tree) userObject).getType());
            System.out.println(((Tree) userObject).getId());
            TestCaseEditor.open(((Tree) userObject).getId());
        }
    }
}
