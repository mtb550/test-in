package org.testin.editor.test;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.automate.AutomateTestCaseAction;
import org.testin.clipboard.CopyTestCaseAction;
import org.testin.clipboard.CopyTestCaseNodeAction;
import org.testin.clipboard.CutTestCaseNodeAction;
import org.testin.clipboard.PasteTestCaseNodeAction;
import org.testin.clipboard.RedoNodeAction;
import org.testin.clipboard.UndoNodeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.editor.statusbar.NextPageAction;
import org.testin.editor.statusbar.PrevPageAction;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.navigate.NavigateToCodeAction;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCaseAction;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.RemoveTestCaseAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;
import org.testin.util.OptionalPlugin;
import org.testin.view.ViewDetailsAction;

public class TestEditorContextMenu extends AbstractEditorContextMenu {

    private final @NotNull Project p;

    /**
     * Kept, unlike the others below, because the field letters run through it:
     * pressing a field's letter on a selected card is the same update this
     * action performs, started at that field instead of at the menu.
     */
    private final @NotNull UpdateTestCaseAction update;

    public TestEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Test Editor Context Menu", true);
        this.p = p;

        add(new CreateTestCaseAction(p, ui, dir, list));
        add(new ViewDetailsAction(p, list, dir.getPath2()));

        addSeparator();

        this.update = new UpdateTestCaseAction(p, ui, list, dir.getPath());
        add(update);
        add(new CopyTestCaseAction(p, list));
        add(new CopyTestCaseNodeAction(p, list));
        add(new CutTestCaseNodeAction(p, ui, list));
        add(new PasteTestCaseNodeAction(p, ui, list));
        add(new RemoveTestCaseAction(p, ui, dir, list, model));

        addSeparator();

        // The same two actions the tree offers, on the same stack, registered on
        // this list as well - so CTRL+Z takes back the last thing the tester
        // did, wherever they were standing when they did it (#165).
        add(new UndoNodeAction(p, list));
        add(new RedoNodeAction(p, list));

        // Absent, not present and broken: in an IDE without the Java or TestNG
        // plugin these three cannot do anything, and a tester should not find
        // them on the menu to be told so by a balloon (#66).
        if (OptionalPlugin.JAVA.isAvailable() || OptionalPlugin.TESTNG.isAvailable()) {
            addSeparator();

            if (OptionalPlugin.JAVA.isAvailable()) add(new AutomateTestCaseAction(p, list));
            if (OptionalPlugin.TESTNG.isAvailable()) add(new RunTestCaseAction(p, ui, list));
            if (OptionalPlugin.JAVA.isAvailable()) add(new NavigateToCodeAction(p, list));
        }

        addSeparator();

        add(new NextPageAction(ui, list));
        add(new PrevPageAction(ui, list));
    }

    /**
     * Each of these registers its own shortcut on the list from its constructor,
     * so the action object is not needed afterward and is deliberately
     * discarded. It reads like a mistake and is not one — the alternative is a
     * factory method per action whose only job is to return something for the
     * caller to ignore.
     */
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list, final @NotNull AbstractEditorContextMenu menu) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(list, menu);

        // Every field the update menu offers, on the letter the menu already
        // shows beside it, so a selected card opens that field's editor with no
        // menu in between: D for the description, S for the steps, and so on.
        //
        // Bound from the field rather than from an action class per field. The
        // enum already owns which letter is which and already knows how to put
        // it on a component - it is the same call the menu popup makes to bind
        // the same letters - so a card and the menu cannot come to disagree
        // about what a letter means (#162).
        //
        // Not menu rows: nine more entries would double this menu, and F2
        // already lists them all. That does keep them off the grid view, which
        // binds what the menu holds.
        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values()) {
            field.bindShortcut(list, () -> update.openField(field));
        }
    }
}