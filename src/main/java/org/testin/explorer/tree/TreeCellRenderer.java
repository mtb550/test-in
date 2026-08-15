package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.DirectoryType;
import org.testin.enums.TestRunStatus;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;

import javax.swing.*;
import java.util.Set;

@AllArgsConstructor
public class TreeCellRenderer extends ColoredTreeCellRenderer {
    /**
     * Shared with the transfer handler: the nodes currently cut, drawn grayed.
     */
    private final @NotNull Set<DirectoryDto> selectedNodes;

    @Override
    public void customizeCellRenderer(final @NotNull JTree tree, final @Nullable Object value, final boolean selected,
                                      final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            if (TreeValueUtil.valueOf(value) instanceof TreeLoadError(String message)) {
                setIcon(AllIcons.General.Error);
                append(message, SimpleTextAttributes.ERROR_ATTRIBUTES);
                return;
            }
            final DirectoryDto dir = TreeValueUtil.directoryOf(value);
            if (dir == null) {
                append(value != null ? value.toString() : "", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }
            final DirectoryType type = dir.getType();

            final TestRunStatus runStatus = dir instanceof TestRunDirectoryDto trDir ? trDir.getMarker().getStatus() : null;

            // A run is drawn as its status, not as its kind: the tree then says
            // where every cycle stands without opening any of them. Every other
            // node takes the icon of what it is.
            setIcon(runStatus != null ? runStatus.getIcon() : type.getIcon());
            append(dir.getName(), selectedNodes.contains(dir) ? SimpleTextAttributes.GRAYED_ATTRIBUTES : type.getAttributes());
            append(" ");
            append(runStatus != null ? runStatus.getLabel() : "", SimpleTextAttributes.GRAY_ATTRIBUTES);

        } catch (final Exception ex) {
            Logger.error("Error rendering tree node: " + ex.getMessage());
            setIcon(AllIcons.General.Error);
            append(value != null ? value.toString() : "Error", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    // todo, if (dir instanceof TestSetDirectoryDto setDir) {
    // todo, later, make a tag for test set if it is approved or still, need to set business and plan before implement
}

