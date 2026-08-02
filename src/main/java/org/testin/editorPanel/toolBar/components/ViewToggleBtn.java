package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ViewToggleBtn extends AbstractButton implements IToolbarItem {

    private final @NotNull Icon listIcon;

    private final @NotNull Icon gridIcon;

    @Getter
    private boolean isGridViewActive;

    public ViewToggleBtn() {
        super("Switch to Grid View", AllIcons.Nodes.DataTables);
        this.listIcon = AllIcons.Actions.ListFiles;
        this.gridIcon = AllIcons.Nodes.DataTables;
        this.isGridViewActive = false;

        addActionListener(e -> toggleView());
    }

    private void toggleView() {
        if (isGridViewActive) {
            setIcon(listIcon);
            setToolTipText("Switch to List View");
            // TODO: switch editor to grid view

        } else {
            setIcon(gridIcon);
            setToolTipText("Switch to Grid View");
            // TODO: switch editor to list view
        }
    }

}
