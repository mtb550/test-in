package testGit.projectPanel.testRunTab;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.editorPanel.testRunEditor.TestRunEditor;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MouseAdapterImpl extends MouseAdapter {
    private final SimpleTree tree;
    private final ProjectPanel projectPanel;

    public MouseAdapterImpl(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = projectPanel.getTestRunTabController().getTree();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("TestRunMouseAdapter.mouseClicked()");

        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selPath == null) {
            System.out.println("selPath == null");
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem)) {
            System.out.println("userObject not instanceof Directory");
            return;
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            System.out.println("right click test run");

            int row = tree.getClosestRowForLocation(e.getX(), e.getY());
            tree.setSelectionRow(row);

            ContextMenu contextMenu = new ContextMenu(projectPanel);
            ActionPopupMenu popupMenu = ActionManager.getInstance()
                    .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu);

            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && treeItem.getType() == DirectoryType.TR) {
            System.out.println("double left click test run");
            TestRunEditor.open(treeItem.getFilePath(), projectPanel);
        }
    }
}
