package testGit.projectPanel.testCaseTab;

import com.intellij.icons.AllIcons;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import testGit.pojo.Directory;
import testGit.projectPanel.Shortcuts;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Custom TreeCellRenderer for IntelliJ UI components.
 * Separates icon logic and text styling for better maintainability.
 */
public class TestCaseRenderer extends SimpleColoredComponent implements TreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        this.clear();

        Object userObject = (value instanceof DefaultMutableTreeNode node) ? node.getUserObject() : null;

        if (userObject instanceof Directory dir) {
            renderDirectory(dir);

        } else if (value != null) {
            setIcon(AllIcons.Nodes.Unknown);
            append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        return this;
    }

    private void renderDirectory(Directory dir) {
        setIcon(getIconForDirectory(dir));
        SimpleTextAttributes style = SimpleTextAttributes.REGULAR_ATTRIBUTES;

        if (dir.getFilePath() != null && Shortcuts.isCutNode(dir.getFilePath())) {
            style = SimpleTextAttributes.GRAYED_ATTRIBUTES;
        }

        append(dir.getName() != null ? dir.getName() : "Unnamed", style);
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