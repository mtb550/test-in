package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;


public abstract class AbstractEditorContextMenu extends DefaultActionGroup {

    public AbstractEditorContextMenu(final @NotNull String name, final boolean popup) {
        super(name, popup);
    }

    public abstract void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}