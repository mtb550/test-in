package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import org.testin.util.Bundle;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class ListViewBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-016
    public ListViewBtn(final @NotNull Runnable onSwitchToList) {
        // https://intellij-icons.jetbrains.design/
        super(Bundle.message("toolbar.list.view"), AllIcons.General.LayoutEditorOnly);

        addActionListener(e -> onSwitchToList.run());
    }
}
