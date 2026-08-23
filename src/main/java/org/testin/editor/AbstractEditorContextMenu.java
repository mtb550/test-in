package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.grid.NotWhileEditing;
import org.testin.model.dto.TestCaseDto;


public abstract class AbstractEditorContextMenu extends DefaultActionGroup {

    public AbstractEditorContextMenu(final @NotNull String name, final boolean popup) {
        super(name, popup);
    }

    public abstract void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu);

    /**
     * Every shortcut this menu offers, live on the grid as well as on the list
     * (#74).
     * <p>
     * The actions bind themselves to the list when the menu is built, and
     * switching to grid view takes the list out of the component tree, so all of
     * them went quiet - a tester could set a status from the grid with the mouse
     * but not with the keyboard, which is the wrong way round for the fastest
     * part of the job.
     * <p>
     * Bound from the menu rather than one action at a time, so what the right
     * button offers and what the keyboard offers cannot drift apart: an action
     * added to the menu is live in both views by being on the menu.
     */
    public void bindShortcutsTo(final @NotNull JBTable table) {
        for (final AnAction action : getChildActionsOrStubs()) {
            NotWhileEditing.bind(table, action);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}