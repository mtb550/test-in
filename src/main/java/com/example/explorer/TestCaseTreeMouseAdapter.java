package com.example.explorer;

import com.example.explorer.testPlan.TestPlanContextMenu;
import com.example.explorer.testPlan.TestRunEditor;
import com.example.pojo.TestPlan;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.project.Project;

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

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        // CASE: it's a TestPlan node (folder or test run)
        if (userObject instanceof TestPlan plan) {
            if (SwingUtilities.isRightMouseButton(e)) {
                JPopupMenu popup = TestPlanContextMenu.create(tree, plan, node);
                popup.show(tree, e.getX(), e.getY());
                return;
            }

            if (plan.getType() == 1 && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                Project project = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects()[0]; // or however you obtain it
                TestRunEditor.open(project, plan.getId(), plan.getName());

            }

            return;
        }

        // CASE: regular test case nodes
        //if (!(userObject instanceof Tree treeItem)) return;

        if (SwingUtilities.isRightMouseButton(e)) {
            ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("TestTreeContextMenuGroup");
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group);
            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

//        } else if (e.getClickCount() == NodeType.FEATURE.getCode() && treeItem.getType() == NodeType.FEATURE.getCode()) {
//            TestCaseEditor.open(treeItem.getId());
        }
    }

}
