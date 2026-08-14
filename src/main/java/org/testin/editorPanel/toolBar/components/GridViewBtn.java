package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class GridViewBtn extends AbstractButton implements IToolbarItem {

    public GridViewBtn(final @NotNull Runnable onSwitchToGrid) {
        // https://intellij-icons.jetbrains.design/
        super("Grid View", AllIcons.General.Groups);

        addActionListener(e -> onSwitchToGrid.run());
    }
}
