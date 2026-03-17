package testGit.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestCasesDirectory;
import testGit.pojo.TestRunsDirectory;

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
        if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Directory dir) {

            DirectoryType type = DirectoryType.fromClass(dir.getClass());
            System.out.println(dir.getClass());
            setIcon(type.getIcon());

            boolean isHeader = (dir instanceof TestCasesDirectory || dir instanceof TestRunsDirectory);
            boolean isCut = (!isHeader && cutNodes != null && cutNodes.contains(node));

            SimpleTextAttributes attrs = isHeader ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES :
                    (isCut ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES);

            append(dir.getName(), attrs);
            return;
        }

        setIcon(AllIcons.Nodes.Unknown);
        append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
}