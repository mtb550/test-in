package org.testin.editorPanel;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

public class EditorContextMenu extends DefaultActionGroup {

    public EditorContextMenu(final @NotNull String shortName, final @NotNull Boolean popup) {
        super(shortName, popup);
    }
}