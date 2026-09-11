package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.testin.util.Bundle;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class GridViewBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-017
    public GridViewBtn(final @NotNull Runnable onSwitchToGrid) {
        // https://intellij-icons.jetbrains.design/
        super(Bundle.message("toolbar.grid.view"), AllIcons.General.Groups);

        addActionListener(e -> onSwitchToGrid.run());
    }
}
