package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import org.testin.editor.grid.GridExcelBehavior;
import java.util.Arrays;
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
            if (claimedByTheGrid(action)) continue;

            NotWhileEditing.bind(table, action);
        }
    }

    /**
     * Whether this action's key is one the grid answers for itself.
     * <p>
     * The same keystrokes carry different meanings on the two views, and binding
     * a menu entry to the table settles it the wrong way round - the IDE
     * dispatches a registered shortcut before a component's own input map, so
     * the menu's version wins rather than competes.
     * <p>
     * Every action stays on the menu and still acts on the test case there. It is
     * only the key the grid keeps, and only while a grid is on screen.
     */
    private static boolean claimedByTheGrid(final @NotNull AnAction action) {
        return Arrays.stream(action.getShortcutSet().getShortcuts())
                .filter(KeyboardShortcut.class::isInstance)
                .map(shortcut -> ((KeyboardShortcut) shortcut).getFirstKeyStroke())
                .anyMatch(GridExcelBehavior.keysTheGridKeeps()::contains);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}