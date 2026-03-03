package testGit.projectPanel.testCaseTab;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.Directory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Set;

public class TestCaseRenderer extends SimpleColoredComponent implements TreeCellRenderer {
    private final Set<DefaultMutableTreeNode> cutNodes;

    public TestCaseRenderer(Set<DefaultMutableTreeNode> cutNodes) {
        this.cutNodes = cutNodes;
        setOpaque(true);
        System.out.println("TestCaseRenderer.TestCaseRenderer");

    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        this.clear();
        Object userObject = (value instanceof DefaultMutableTreeNode node) ? node.getUserObject() : value;

        if (userObject instanceof Directory dir) {
            System.out.println("Rendering TC: " + dir.getName());
            renderDirectory((value instanceof DefaultMutableTreeNode n) ? n : null, dir);

        } else if (value != null) {
            setIcon(AllIcons.Nodes.Unknown);
            append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        if (selected) {
            setBackground(UIUtil.getTreeSelectionBackground(true));
            setForeground(UIUtil.getTreeSelectionForeground(true));

        } else {
            setBackground(UIUtil.getTreeBackground());
            setForeground(UIUtil.getTreeForeground());
        }

        return this;
    }

    private void renderDirectory(DefaultMutableTreeNode node, Directory dir) {
        setIcon(getIconForDirectory(dir));


        append(dir.getName(), (cutNodes.contains(node)) ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    private Icon getIconForDirectory(Directory dir) {
        return switch (dir.getType()) {
            case PR -> AllIcons.Nodes.Project;
            case PA -> AllIcons.Nodes.WebFolder;
            case TS -> AllIcons.Nodes.Class;
            default -> AllIcons.Nodes.Unknown;
        };
    }
}