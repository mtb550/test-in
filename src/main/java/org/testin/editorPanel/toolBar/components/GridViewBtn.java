package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class GridViewBtn extends AbstractButton implements IToolbarItem {

    public GridViewBtn(final Runnable onSwitchToGrid) {
        super("Switch to Grid View", AllIcons.Nodes.DataTables);

        addActionListener(e -> onSwitchToGrid.run());
        // TODO: switch editor to grid view
    }
}
