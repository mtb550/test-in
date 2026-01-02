package com.example.explorer;

import com.example.editor.TestCaseEditor;
import com.example.explorer.testPlan.TestPlanContextMenu;
import com.example.explorer.testPlan.TestRunEditor;
import com.example.pojo.Config;
import com.example.pojo.Directory;
import com.example.pojo.TestPlan;
import com.example.util.NodeType;
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
    private final ExplorerPanel panel;

    public TestCaseTreeMouseAdapter(JTree tree, ExplorerPanel panel) {
        this.tree = tree;
        this.panel = panel;
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
                TestRunEditor.open(Config.getProject(), plan.getId(), plan.getName());

            }

            return;
        }

        // CASE: regular test case nodes
        // التأكد من أن الكائن هو Directory (مشروع أو مجلد أو Feature)
        if (!(userObject instanceof Directory treeItem)) return;

// 1. التعامل مع الزر الأيمن (القائمة المنبثقة)
        if (SwingUtilities.isRightMouseButton(e)) {
            TestCaseContext contextMenu = new TestCaseContext(panel);
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(
                    ActionPlaces.TOOLWINDOW_POPUP,
                    contextMenu
            );
            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());
        }
// 2. التعامل مع الضغط المزدوج بالزر الأيسر لفتح المحرر
        else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
            // نتحقق هنا من أن نوع المجلد هو "Feature"
            if (treeItem.getType() == NodeType.FEATURE.getCode()) {
                TestCaseEditor.open(treeItem.getFilePath());
            }
        }
    }

}
