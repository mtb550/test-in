package testGit.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.DirectoryType;
import testGit.pojo.tree.dirs.Directory;
import testGit.pojo.tree.dirs.TestCasesDirectory;
import testGit.pojo.tree.dirs.TestRunsDirectory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;

public class TreeCellRenderer extends ColoredTreeCellRenderer {
    private final Set<DefaultMutableTreeNode> selectedNodes;

    public TreeCellRenderer(final Set<DefaultMutableTreeNode> selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    @Override
    public void customizeCellRenderer(@NotNull final JTree tree, final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            switch (value) {
                case DefaultMutableTreeNode node when node.getUserObject() instanceof Directory dir -> {
                    DirectoryType type = DirectoryType.fromClass(dir.getClass());
                    setIcon(type != null ? type.getIcon() : AllIcons.Nodes.Folder);
                    append(dir.getName(), getSimpleTextAttributes(node, dir));
                }

                case DefaultMutableTreeNode node -> {
                    setIcon(AllIcons.Nodes.Unknown);
                    Object obj = node.getUserObject();
                    append(obj != null ? obj.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }

                default -> {
                    setIcon(AllIcons.Nodes.Unknown);
                    append(value.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }

        } catch (Exception e) {
            System.err.println("Error rendering tree node: " + e.getMessage());
            setIcon(AllIcons.General.Error);
            append(value != null ? value.toString() : "Error", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    private @NotNull SimpleTextAttributes getSimpleTextAttributes(final DefaultMutableTreeNode node, final Directory dir) {
        return switch (dir) {
            case TestCasesDirectory ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            case TestRunsDirectory ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            default -> (selectedNodes != null && selectedNodes.contains(node))
                    ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                    : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        };
    }
}