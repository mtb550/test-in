package org.testin.editor.toolbar.components;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class ListViewBtn extends AbstractButton implements ToolbarItem {

    public ListViewBtn(final @NotNull Runnable onSwitchToList) {
        // https://intellij-icons.jetbrains.design/
        super("List View", AllIcons.General.LayoutEditorOnly);

        addActionListener(e -> onSwitchToList.run());
    }
}
