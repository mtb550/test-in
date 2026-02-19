package testGit.projectPanel.testRunTab;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestRunStatus;
import testGit.projectPanel.Shortcuts;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Custom TreeCellRenderer for IntelliJ UI components.
 * Separates icon logic and text styling for better maintainability.
 */
public class TestRunRenderer extends SimpleColoredComponent implements TreeCellRenderer {

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
        System.out.println("TestRunRenderer.renderDirectory()");

        setIcon(getIconForDirectory(dir));
        SimpleTextAttributes style = SimpleTextAttributes.REGULAR_ATTRIBUTES;

        if (dir.getFilePath() != null && Shortcuts.isCutNode(dir.getFilePath())) {
            style = SimpleTextAttributes.GRAYED_ATTRIBUTES;
        }

        append(dir.getName() != null ? dir.getName() : "Unnamed", style);

        if (dir.getType() == DirectoryType.TR) {
            String statusLabel = TestRunStatus.labelFor(dir.getActive());

            SimpleTextAttributes statusStyle = new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.DARK_GRAY);
            append(" " + statusLabel, statusStyle);
        }
    }

    private Icon getIconForDirectory(Directory dir) {
        return switch (dir.getType()) {
            case PR -> AllIcons.Nodes.Project;
            case PA -> AllIcons.Nodes.WebFolder;
            case TR -> AllIcons.Nodes.Services;
            default -> AllIcons.Nodes.Folder;
        };
    }
}