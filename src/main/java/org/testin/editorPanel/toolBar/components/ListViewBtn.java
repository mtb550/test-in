package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;

public class ListViewBtn extends AbstractButton implements IToolbarItem {

    public ListViewBtn(final Runnable onSwitchToList) {
        super("List View", AllIcons.Actions.ListFiles);

        addActionListener(e -> onSwitchToList.run());
    }
}
