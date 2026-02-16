package testGit.projectPanel.testCaseTab;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.treeStructure.SimpleTree;
import testGit.editorPanel.testCaseEditor.TestCaseEditor;
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

    public MouseAdapterImpl(final ProjectPanel projectPanel) {
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

        if (!(userObject instanceof Directory treeItem)) return;

        if (SwingUtilities.isRightMouseButton(e)) {
            ContextMenu contextMenu = new ContextMenu(projectPanel);
            ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, contextMenu);
            popupMenu.getComponent().show(e.getComponent(), e.getX(), e.getY());

        } else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && treeItem.getType() == DirectoryType.TS)
            TestCaseEditor.open(treeItem);


    }

}
