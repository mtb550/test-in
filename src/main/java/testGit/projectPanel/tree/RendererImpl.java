package testGit.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Set;

public class RendererImpl extends ColoredTreeCellRenderer {
    private final Set<DefaultMutableTreeNode> cutNodes;

    public RendererImpl(Set<DefaultMutableTreeNode> cutNodes) {
        this.cutNodes = cutNodes;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Object userObject = (value instanceof DefaultMutableTreeNode node) ? node.getUserObject() : value;

        if (userObject instanceof TestPackage pkg) {

            setIcon(pkg.getIcon().getValue());

            if (pkg.getType() == DirectoryType.TCP || pkg.getType() == DirectoryType.TRP) {
                append(pkg.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

            } else {
                boolean isCut = (cutNodes != null && value instanceof DefaultMutableTreeNode && cutNodes.contains(value));
                append(pkg.getName(), isCut ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            return;
        }

        setIcon(AllIcons.Nodes.Unknown);
        append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
}