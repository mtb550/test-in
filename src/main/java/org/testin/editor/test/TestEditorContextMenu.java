package org.testin.editor.test;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;
import org.testin.EscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.open.OpenContextMenuAction;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;

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
        add(Declared.action("Testin.ViewDetails"));

        addSeparator();

        this.update = new UpdateTestCaseAction(p, ui, list, dir.getPath());
        add(update);

        add(actions(p, ui, dir, list, model));

        // Present and grayed, with the reason on the entry. This is the reverse
        // of what #66 decided - absent rather than offered and refused - and the
        // reverse is deliberate: a menu that changes shape between IDEs teaches
        // a tester nothing, and they cannot learn the feature exists or what to
        // install. Each action grays itself, because what it needs is its own
        // knowledge: deciding it here is how Run came to be offered in an IDE
        // that could not resolve a single case (#248).
        addSeparator();

        add(Declared.action("Testin.AutomateTestCase"));
        add(Declared.action("Testin.RunTestCase"));
        add(Declared.action("Testin.NavigateToCode"));

    }


    /**
     * UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194.
     * <p>
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