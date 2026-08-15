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
import javax.swing.event.DocumentEvent;

/**
 * A large input field on its own — the component of the rename dialog and any
 * other single-value input. Same look and empty-submit cue as
 * {@link TextFieldWithSelections}, without the selection list.
 */
public final class TextInput implements DialogComponent {

    private final @NotNull ExtendableTextField textField;
    private final @Nullable String placeHolderText;
    private boolean emptyWarningShown;

    TextInput(final @Nullable Icon icon, final @Nullable String placeHolderText, final @Nullable String initialValue) {
        this.placeHolderText = placeHolderText;
        textField = new ExtendableTextField(initialValue == null ? "" : initialValue);
        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        textField.setFont(JBFont.label().biggerOn(6f));
        textField.setBorder(JBUI.Borders.empty(10, 12));

        if (placeHolderText != null && !placeHolderText.isBlank()) {
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
        if (placeHolderText == null || placeHolderText.isBlank()) return;

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
