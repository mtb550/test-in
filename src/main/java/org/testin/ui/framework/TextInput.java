package org.testin.ui.framework;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.event.DocumentEvent;

/**
 * A large input field on its own — the component of the rename dialog and any
 * other single-value input. Same look and empty-submit cue as
 * {@link TextFieldWithSelections}, without the selection list.
 */
public final class TextInput implements DialogComponent {

    /**
     * What a field with no rule accepts: anything at all, and no filter is
     * installed for it.
     */
    static final @NotNull String ANYTHING = ".*";

    private final @NotNull ExtendableTextField textField;
    private final @NotNull String placeHolderText;
    private boolean emptyWarningShown;

    /**
     * @param accepts what the field is allowed to hold, as a regular expression
     *                the whole text must match. Checked as the text arrives, so
     *                a field that means a number, a version or an id cannot be
     *                made to hold anything else - there is nothing to validate
     *                on submit and nothing to explain afterward
     */
    TextInput(final @NotNull Icon icon, final @NotNull String placeHolderText, final @NotNull String initialValue,
              final @NotNull String accepts) {
        this.placeHolderText = placeHolderText;
        textField = new ExtendableTextField(initialValue);
        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        textField.setFont(JBFont.label().biggerOn(6f));
        textField.setBorder(JBUI.Borders.empty(10, 12));

        if (!placeHolderText.isBlank()) {
            textField.getEmptyText().setText(placeHolderText);
            TextComponentEmptyText.setupPlaceholderVisibility(textField);
            // Typing clears a red empty-submit warning back to the normal look.
            textField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(final @NotNull DocumentEvent e) {
                    if (emptyWarningShown) {
                        emptyWarningShown = false;
                        showPlaceholder(SimpleTextAttributes.GRAYED_ATTRIBUTES);
                    }
                }
            });
        }
        DialogStyle.setLeadingIcon(textField, icon);

        if (!ANYTHING.equals(accepts)) accept(Pattern.compile(accepts));
    }

    /**
     * Holds the field to what the pattern allows, by filtering its document -
     * which catches typing, pasting and dropping alike. A key listener would let
     * a paste straight through.
     * <p>
     * The test is on the text the change would produce, not on the characters
     * arriving, so a pattern can say how long a value may be or what shape it
     * has and not merely which letters it is made of. An edit that would break
     * the pattern does not happen: the field simply does not take it, which is
     * how a tester learns the rule without being told it.
     * <p>
     * Emptying the field is always allowed. A rule about what a value looks like
     * is not a rule that there must be one - that is the dialog's decision, and
     * it makes it on submit.
     */
    private void accept(final @NotNull Pattern pattern) {
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(final @NotNull FilterBypass bypass, final int offset, final String text,
                                     final AttributeSet attributes) throws BadLocationException {
                if (allows(bypass, offset, 0, text)) super.insertString(bypass, offset, text, attributes);
            }

            @Override
            public void replace(final @NotNull FilterBypass bypass, final int offset, final int length,
                                final String text, final AttributeSet attributes) throws BadLocationException {
                if (allows(bypass, offset, length, text)) super.replace(bypass, offset, length, text, attributes);
            }

            private boolean allows(final @NotNull FilterBypass bypass, final int offset, final int length,
                                   final @Nullable String text) throws BadLocationException {
                final String current = bypass.getDocument().getText(0, bypass.getDocument().getLength());
                final String next = current.substring(0, offset) + Objects.requireNonNullElse(text, "")
                        + current.substring(offset + length);

                return next.isEmpty() || pattern.matcher(next).matches();
            }
        });
    }

    public @NotNull String getText() {
        return textField.getText();
    }

    /**
     * Turns the placeholder red until the tester types — the empty-submit cue.
     */
    public void showEmptyWarning() {
        emptyWarningShown = true;
        showPlaceholder(SimpleTextAttributes.ERROR_ATTRIBUTES);
        textField.requestFocusInWindow();
    }

    private void showPlaceholder(final @NotNull SimpleTextAttributes attributes) {
        if (placeHolderText.isBlank()) return;

        textField.getEmptyText().clear();
        textField.getEmptyText().appendText(placeHolderText, attributes);
        textField.repaint();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return textField;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return textField;
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        // The field has no submit gesture of its own; Enter is a declared key.
    }
}
