package testGit.projectPanel.testCase;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.editorPanel.TestCaseEditor;
import testGit.pojo.Directory;
import testGit.pojo.TestPlan;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.testPlan.TestPlanContext;
import testGit.util.NodeType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TestCaseMouseAdapter extends MouseAdapter {
    private final SimpleTree tree;
    private final ProjectPanel projectPanel;

    public TestCaseMouseAdapter(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getTestCaseTree();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("ProjectPanelMouseAdapter.mouseClicked()");

        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        // CASE: it's a TestPlan node (folder or test run)
        if (userObject instanceof TestPlan plan) {
            if (SwingUtilities.isRightMouseButton(e)) {
                JPopupMenu popup = TestPlanContext.create(tree, plan, node);
                popup.show(tree, e.getX(), e.getY());
                return;
            }

            //if (plan.getType() == 1 && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            //TestRunEditor.open(Config.getProject(), plan.getId(), plan.getName());

            // }

            return;
        }

        // CASE: regular test case nodes
        // التأكد من أن الكائن هو Directory (مشروع أو مجلد أو Feature)
        if (!(userObject instanceof Directory treeItem)) return;

// 1. التعامل مع الزر الأيمن (القائمة المنبثقة)
        if (SwingUtilities.isRightMouseButton(e)) {
            TestCaseContext contextMenu = new TestCaseContext(projectPanel);
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
