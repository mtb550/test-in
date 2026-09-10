package org.testin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import org.testin.clipboard.PasteTestCaseNodeAction;
import org.testin.actions.Declared;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.ui.ActionsMenu;
import org.testin.testcase.RemoveTestCaseAction;
import org.testin.undo.UndoAction;
import org.testin.undo.UndoDirection;
import org.testin.undo.UndoScope;
import com.intellij.ui.components.JBList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import org.testin.editor.grid.GridKeys;
import java.util.Arrays;
import org.testin.editor.grid.NotWhileEditing;
import org.testin.model.dto.TestCaseDto;


public abstract class AbstractEditorContextMenu extends DefaultActionGroup {

    public AbstractEditorContextMenu(final @NotNull String name, final boolean popup) {
        super(name, popup);
    }

    public abstract void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu);

    /**
     * UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-213.
     * <p>
     * Everything done <em>to</em> a test case, one level down: the clipboard,
     * the delete, and this editor's own history.
     * <p>
     * Seven entries of the twelve were these, and none of them is what a tester
     * opens the menu for - they all have keys, and the keys are what anybody
     * uses after the first week. At the top level they pushed Automate, Run and
     * Navigate to Code, which is what this plugin is for, off the end of a list
     * nobody read that far down.
     * <p>
     * <b>The same seven in both editors</b>, which is why this is here and not in
     * either menu. A test run cannot hold or lose a test case, so Cut, Paste and
     * Delete are gray there with the reason on the entry - shown and refused
     * rather than left out, so a tester learns the gesture exists and where it
     * does work. Each entry decides that for itself, from the node the editor is
     * open on.
     */
    protected @NotNull DefaultActionGroup actions(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        final @NotNull DefaultActionGroup actions = ActionsMenu.group();

        actions.add(Declared.action("Testin.CopyTestCase"));
        actions.add(Declared.action("Testin.CopyTestCaseNode"));
        actions.add(Declared.action("Testin.CutTestCaseNode"));
        actions.add(new PasteTestCaseNodeAction(p, ui, list));
        actions.add(new RemoveTestCaseAction(p, ui, dir, list, model));

        actions.addSeparator();

        // This editor's own history, not the tree's and not another editor's:
        // two cases removed here come back here, and the two removed in the
        // editor beside it come back there (#165).
        actions.add(new UndoAction(p, list, UndoScope.of(dir.getPath()), UndoDirection.UNDO));
        actions.add(new UndoAction(p, list, UndoScope.of(dir.getPath()), UndoDirection.REDO));

        return actions;
    }

    /**
     * Rule-EDITOR-PANEL-010.
     * <p>
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
        bindGroup(this, table);
    }

    /**
     * The group's entries, and the entries of any group inside it.
     * <p>
     * A nested group is one child to the loop above and seven keys to a tester,
     * so walking only the top level would take Copy, Cut, Paste, Delete, Undo
     * and Redo off the grid the day they were gathered under one entry - and
     * silently, because a shortcut that is never registered fails by doing
     * nothing.
     */
    private static void bindGroup(final @NotNull DefaultActionGroup group, final @NotNull JBTable table) {
        for (final AnAction action : group.getChildActionsOrStubs()) {
            if (action instanceof DefaultActionGroup nested) {
                bindGroup(nested, table);
                continue;
            }

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
                .anyMatch(GridKeys.keptFromMenus()::contains);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}