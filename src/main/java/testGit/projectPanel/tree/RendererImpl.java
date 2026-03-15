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
        if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TestPackage pkg) {

            setIcon((pkg.getIcon() != null && pkg.getIcon().getValue() != null) ? pkg.getIcon().getValue() : AllIcons.Nodes.Folder);

            boolean isHeader = (pkg.getType() == DirectoryType.TCP || pkg.getType() == DirectoryType.TRP);
            boolean isCut = (!isHeader && cutNodes != null && cutNodes.contains(node));

            SimpleTextAttributes attrs = isHeader ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES :
                    (isCut ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);

            append(pkg.getName(), attrs);
            return;
        }

        setIcon(AllIcons.Nodes.Unknown);
        append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
}