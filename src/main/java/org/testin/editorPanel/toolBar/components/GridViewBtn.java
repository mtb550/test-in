package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class GridViewBtn extends AbstractButton implements IToolbarItem {

    public GridViewBtn(final @NotNull Runnable onSwitchToGrid) {
        super("Grid View", AllIcons.Nodes.DataTables);

        addActionListener(e -> onSwitchToGrid.run());
    }
}
