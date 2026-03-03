package testGit.projectPanel.testRunTab;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestRunStatus;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class TestRunRenderer extends SimpleColoredComponent implements TreeCellRenderer {

    public TestRunRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        this.clear();

        Object userObject = (value instanceof DefaultMutableTreeNode node) ? node.getUserObject() : value;

        if (userObject instanceof Directory dir) {
            System.out.println("Rendering TR: " + dir.getName());
            renderDirectory(dir);
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

    private void renderDirectory(Directory dir) {
        setIcon(getIconForDirectory(dir));
        append(dir.getName() != null ? dir.getName() : "Unnamed", SimpleTextAttributes.REGULAR_ATTRIBUTES);

        if (dir.getType() == DirectoryType.TR) {
            String statusLabel = TestRunStatus.labelFor(dir.getActive());
            append(" " + statusLabel, new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.DARK_GRAY));
        }
    }

    private Icon getIconForDirectory(Directory dir) {
        if (dir.getType() == null) return AllIcons.Nodes.Folder;
        return switch (dir.getType()) {
            case PR -> AllIcons.Nodes.Project;
            case PA -> AllIcons.Nodes.WebFolder;
            case TR -> AllIcons.Nodes.Services;
            default -> AllIcons.Nodes.Folder;
        };
    }
}