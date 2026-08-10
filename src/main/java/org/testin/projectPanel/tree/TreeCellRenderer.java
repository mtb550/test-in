package org.testin.projectPanel.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;

import javax.swing.*;
import java.util.Set;

public class TreeCellRenderer extends ColoredTreeCellRenderer {
    private final Set<DirectoryDto> selectedNodes;

    public TreeCellRenderer(final Set<DirectoryDto> selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    @Override
    public void customizeCellRenderer(final @NotNull JTree tree, final Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            if (TreeValueUtil.valueOf(value) instanceof TreeLoadError(String message)) {
                setIcon(AllIcons.General.Error);
                append(message, SimpleTextAttributes.ERROR_ATTRIBUTES);
                return;
            }
            DirectoryDto dir = TreeValueUtil.directoryOf(value);
            if (dir == null) {
                append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }
            DirectoryType type = DirectoryType.from(dir);

            setIcon(type.getIcon());
            append(dir.getName(), (selectedNodes != null && !selectedNodes.contains(dir)) ? type.getAttributes() : SimpleTextAttributes.GRAYED_ATTRIBUTES);
            append(" ");
            append(dir instanceof TestRunDirectoryDto trDir ? trDir.getMarker().getStatus().getLabel() : "", SimpleTextAttributes.GRAY_ATTRIBUTES);

        } catch (final Exception ex) {
            Logger.error("Error rendering tree node: " + ex.getMessage());
            setIcon(AllIcons.General.Error);
            append(value != null ? value.toString() : "Error", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }


    // todo, if (dir instanceof TestSetDirectoryDto setDir) {
    // todo, later, make a tag for test set if it is approved or still, need to set business and plan before implement
}
