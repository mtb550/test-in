package org.testin.editorPanel.toolBar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class ListViewBtn extends AbstractButton implements IToolbarItem {

    public ListViewBtn(final @NotNull Runnable onSwitchToList) {
        super("List View", AllIcons.Actions.ListFiles);

        addActionListener(e -> onSwitchToList.run());
    }
}
