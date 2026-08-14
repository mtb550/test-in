package org.testin.ui.framework;

import com.intellij.ui.components.fields.ExtendableTextField;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static org.testng.Assert.*;

/**
 * Behavior of the framework components themselves: initial selection, arrow
 * navigation with clamping, the cleared-selection fallback, the text input's
 * initial value and placeholder, and the message's From/To rows.
 */
public class FrameworkComponentsTest {

    private static TextFieldWithSelections<Integer> twoSelections() {
        return ComponentDialogBase.<Integer>textFieldWithSelections()
                .placeholder("set name..")
                .selection(null, "One", "first row", 1)
                .selection(null, "Two", "second row", 2)
                .build()
                .getComponent();
    }

    private static void pressNavigationKey(final TextFieldWithSelections<?> component, final String actionKey) {
        final JComponent field = component.getFocusComponent();
        field.getActionMap().get(actionKey).actionPerformed(new ActionEvent(field, 0, ""));
    }

    private static JList<?> findList(final Container container) {
        for (final Component child : container.getComponents()) {
            if (child instanceof JList<?> list) return list;
            if (child instanceof Container inner) {
                final JList<?> found = findList(inner);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int rowCount(final DialogMessage message) {
        final Container content = (Container) message.getPanel().getComponent(0);
        return content.getComponentCount();
    }

    @Test
    public void selectionStartsOnTheFirstDeclaredRow() {
        assertEquals(twoSelections().getSelectedValue(), Integer.valueOf(1));
    }

    @Test
    public void arrowNavigationMovesAndClampsAtBothEnds() {
        final TextFieldWithSelections<Integer> component = twoSelections();

        pressNavigationKey(component, "testin.framework.selectionDown");
        assertEquals(component.getSelectedValue(), Integer.valueOf(2), "down moves to the second row");

        pressNavigationKey(component, "testin.framework.selectionDown");
        assertEquals(component.getSelectedValue(), Integer.valueOf(2), "down clamps at the last row");

        pressNavigationKey(component, "testin.framework.selectionUp");
        assertEquals(component.getSelectedValue(), Integer.valueOf(1), "up moves back to the first row");

        pressNavigationKey(component, "testin.framework.selectionUp");
        assertEquals(component.getSelectedValue(), Integer.valueOf(1), "up clamps at the first row");
    }

    @Test
    public void clearedSelectionFallsBackToTheFirstRow() {
        final TextFieldWithSelections<Integer> component = twoSelections();

        final JList<?> list = findList(component.getPanel());
        assertNotNull(list, "the selection list must be part of the component panel");
        list.clearSelection();

        // The Ctrl+click case: submitting with an emptied selection must not
        // crash - the first row is the declared default.
        assertEquals(component.getSelectedValue(), Integer.valueOf(1));
    }

    @Test
    public void textInputOpensWithValueAndPlaceholder() {
        final TextInput input = ComponentDialogBase.textField()
                .placeholder("set new name..")
                .value("current name")
                .build()
                .getComponent();

        assertEquals(input.getText(), "current name");
        assertEquals(((ExtendableTextField) input.getFocusComponent()).getEmptyText().getText(), "set new name..");
    }

    @Test
    public void emptyWarningKeepsThePlaceholderText() {
        final TextInput input = ComponentDialogBase.textField().placeholder("set new name..").build().getComponent();

        input.showEmptyWarning();

        // The warning re-renders the placeholder in the error color; the text
        // itself must survive the re-render.
        assertEquals(((ExtendableTextField) input.getFocusComponent()).getEmptyText().getText(), "set new name..");
    }

    @Test
    public void messageShowsFromToRowsOnlyWhenGiven() {
        final DialogMessage plain = ComponentDialogBase.message("Remove 'X'?", null, null).getComponent();
        final DialogMessage transfer = ComponentDialogBase.message("Move 'X' into 'Y'?", "a/b", "a/c").getComponent();

        assertEquals(rowCount(plain), 1, "message only");
        assertEquals(rowCount(transfer), 3, "message + From + To");
    }

    @Test
    public void selectionRowKeepsAllDeclaredParts() {
        final SelectionList<String> row = SelectionList.add(null, "Test Set", "Holds test cases", "TS");

        assertEquals(row.name(), "Test Set");
        assertEquals(row.hint(), "Holds test cases");
        assertEquals(row.value(), "TS");
    }

    @Test
    public void radioSelectionStartsOnTheDeclaredDefault() {
        final RadioSelection<String> radios = ComponentDialogBase.<String>radios("Severity")
                .option("Major", "MAJOR")
                .option("Minor", "MINOR")
                .select("MINOR")
                .build()
                .getComponent();

        assertEquals(radios.getSelected(), "MINOR");
    }

    @Test
    public void radiosRejectEmptyOptionsAndForeignSelection() {
        org.testng.Assert.expectThrows(IllegalStateException.class, () ->
                ComponentDialogBase.<String>radios("x").build());

        org.testng.Assert.expectThrows(IllegalStateException.class, () ->
                ComponentDialogBase.<String>radios("x").option("A", "A").select("B").build());
    }

    @Test
    public void textAreaKeepsValueAndClaimsTheSpace() {
        final TextArea area = ComponentDialogBase.textArea()
                .placeholder("paste error or exception or screenshot..")
                .value("java.lang.RuntimeException")
                .build()
                .getComponent();

        assertEquals(area.getText(), "java.lang.RuntimeException");
        assertTrue(area.fillsSpace(), "the area must claim the dialog's remaining space");
        assertFalse(area.acceptsDialogKeys(), "Enter must stay a newline inside the area");
    }

    @Test
    public void detailsSkipBlankRowsAndNeverWantFocus() {
        final DialogDetails details = ComponentDialogBase.details()
                .row("Description", "shown")
                .row("Expected", "")
                .row("Skipped", null)
                .build()
                .getComponent();

        assertEquals(details.getPanel().getComponentCount(), 1, "blank rows are skipped");
        assertFalse(details.wantsFocus(), "context rows never take the focus");
    }

    /**
     * A dialog that declares no filler at all - a form and a button - used to
     * hand the spare space to its last component, which is the button, putting it
     * in the middle of the dialog instead of at the bottom. It only showed on a
     * dialog with a preferredSize, so nothing caught it.
     */
    @Test
    public void aButtonRowNeverTakesTheDialogSpace() {
        final DialogButton button = ComponentDialogBase.button("Generate").getComponent();

        assertFalse(button.fillsSpace(), "a button row does not claim the space");
        assertFalse(button.canFillSpace(), "and must not be given it when nothing else claims it");
    }

    @Test
    public void everythingElseCanTakeTheSpaceWhenNothingClaimsIt() {
        final TextInput input = ComponentDialogBase.textField().value("name").build().getComponent();
        final TextArea area = ComponentDialogBase.textArea().value("x").build().getComponent();

        assertTrue(input.canFillSpace(), "a field is a reasonable fallback filler");
        assertTrue(area.canFillSpace());
    }

    @Test
    public void builtStatusBarEntryRendersTheKeymapText() {
        final StatusBarShortcut entry = StatusBarShortcut.build(org.testin.util.Shortcuts.Enter, "Confirm", () -> {
        });

        assertTrue(entry.isBindable());
        assertEquals(entry.getName(), "Confirm");
        assertFalse(entry.getShortcutText().isBlank(), "the keystroke must render as text");
    }
}
