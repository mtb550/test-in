package org.testin.ui.framework;

import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.ComponentWithEmptyText;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * How Testin says a field the tester left empty is the one holding the dialog
 * open.
 * <p>
 * One place, because there were two answers to one question. The dialogs built
 * on this framework turned the field's own placeholder red and put the cursor
 * in it; the report and export dialog moved the cursor and did nothing else, so
 * the tester pressed the button, the dialog stayed open, nothing turned red and
 * nothing was said - which reads as a button that does not work (#251).
 * <p>
 * <b>The message goes where the missing value would be.</b> Not a balloon: a
 * balloon appears after the dialog has been dismissed or beside it, and the one
 * thing a tester needs to know is which box is empty. That is also why this
 * takes the words rather than inventing them - "Choose a folder" belongs to the
 * folder field and cannot be written here.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EmptyWarning {

    /**
     * Marks a field as the empty one and puts the cursor in it.
     * <p>
     * A component that cannot draw placeholder text - a combo box, for one -
     * takes the focus and nothing else, which is the whole of what it can show.
     * That is not silence in the same way: a combo the tester has to answer is
     * a list they can see, not a box that looks filled in.
     */
    public static void show(final @NotNull JComponent field, final @NotNull String whatIsMissing) {
        if (field instanceof ComponentWithEmptyText withEmptyText) {
            withEmptyText.getEmptyText().clear();
            withEmptyText.getEmptyText().appendText(whatIsMissing, SimpleTextAttributes.ERROR_ATTRIBUTES);
            field.repaint();
        }

        field.requestFocusInWindow();
    }
}
