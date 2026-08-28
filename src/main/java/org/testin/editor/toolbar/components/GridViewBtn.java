package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class GridViewBtn extends AbstractIconButton implements ToolbarItem {

    public GridViewBtn(final @NotNull Runnable onSwitchToGrid) {
        // https://intellij-icons.jetbrains.design/
        super("Grid View", AllIcons.General.Groups);

        addActionListener(e -> onSwitchToGrid.run());
    }
}
