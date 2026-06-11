package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.util.logger.Log;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Arrays;
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
                case DefaultMutableTreeNode node when node.getUserObject() instanceof DirectoryDto dir -> {

                    DirectoryType type = Arrays.stream(DirectoryType.values())
                            .filter(t -> t.getClazz() == dir.getClass())
                            .findFirst()
                            .orElse(null);

                    setIcon(type != null ? type.getIcon() : AllIcons.Nodes.Folder);
                    append(dir.getName(), getSimpleTextAttributes(node, dir));
                    append(" ");
                    append(dir instanceof TestRunDirectoryDto trDir ? trDir.getMarker().getStatus().name() : "", SimpleTextAttributes.GRAY_ATTRIBUTES);
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
            Log.error("Error rendering tree node: " + e.getMessage());
            setIcon(AllIcons.General.Error);
            append(value != null ? value.toString() : "Error", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    private @NotNull SimpleTextAttributes getSimpleTextAttributes(final DefaultMutableTreeNode node, final DirectoryDto dir) {
        return switch (dir) {
            case TestCasesMainDirectoryDto ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            case TestRunsMainDirectoryDto ignored -> SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

            default -> (selectedNodes != null && selectedNodes.contains(node))
                    ? SimpleTextAttributes.GRAYED_ATTRIBUTES
                    : SimpleTextAttributes.REGULAR_ATTRIBUTES;
        };
    }

    // todo, if (dir instanceof TestSetDirectoryDto setDir) {
    // todo, later, make a tag for test set if it is approved or still, need to set business and plan before implement
}
