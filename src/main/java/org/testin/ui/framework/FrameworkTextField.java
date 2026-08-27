package org.testin.ui.framework;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * The framework's single-line input: its look, its placeholder, and the red cue
 * a dialog shows when the tester submits it empty.
 * <p>
 * One owner because there were two, byte for byte. {@link TextInput} and
 * {@link TextFieldWithSelections} each built the same field the same way and
 * each carried the same placeholder-and-warning pair, so improving the cue -
 * or changing the font, or the border - meant editing two files and noticing
 * that the second one existed.
 * <p>
 * The clipboard bindings are here too, and they are the reason this is worth
 * more than tidiness. A popup or a dialog can eat Ctrl+V, Ctrl+C and Ctrl+X on
 * the way to a field, so they have to be bound on the field itself. Only the
 * search field did that; rename, the commit message, the Git name and email and
 * the SFTP fields did not. The multi-line area was the worst of them: it
 * replaces the paste action to insert a pasted screenshot and never bound the
 * key that reaches it, so its whole image-paste feature rested on a binding the
 * class next door documents as unreliable.
 */
final class FrameworkTextField {

    private final @NotNull ExtendableTextField field;
    private final @NotNull String placeholder;

    private boolean emptyWarningShown;

    FrameworkTextField(final @NotNull Icon icon, final @NotNull String placeholder, final @NotNull String initialValue) {
        this.placeholder = placeholder;
        this.field = new ExtendableTextField(initialValue);

        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        field.setFont(JBFont.label().biggerOn(6f));
        // 12px left rhythm shared by the field text and any list rows below.
        field.setBorder(JBUI.Borders.empty(10, 12));

        if (!placeholder.isBlank()) {
            field.getEmptyText().setText(placeholder);
            TextComponentEmptyText.setupPlaceholderVisibility(field);

            // Typing clears a red empty-submit warning back to the normal look.
            field.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(final @NotNull DocumentEvent e) {
                    if (!emptyWarningShown) return;

                    emptyWarningShown = false;
                    showPlaceholder(SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            });
        }

        DialogStyle.setLeadingIcon(field, icon);
        bindClipboard(field);
    }

    @NotNull ExtendableTextField component() {
        return field;
    }

    @NotNull String getText() {
        return field.getText();
    }

    /**
     * Turns the placeholder red until the tester types - the empty-submit cue.
     */
    void showEmptyWarning() {
        emptyWarningShown = true;
        showPlaceholder(SimpleTextAttributes.ERROR_ATTRIBUTES);
        field.requestFocusInWindow();
    }

    private void showPlaceholder(final @NotNull SimpleTextAttributes attributes) {
        if (placeholder.isBlank()) return;

        field.getEmptyText().clear();
        field.getEmptyText().appendText(placeholder, attributes);
        field.repaint();
    }

    /**
     * Binds cut, copy, paste and select-all on the component itself.
     * <p>
     * Static so the multi-line area can take the same bindings without taking
     * the single-line look with them.
     */
    static void bindClipboard(final @NotNull JTextComponent component) {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        bind(component, KeyEvent.VK_V, menuMask, DefaultEditorKit.pasteAction);
        bind(component, KeyEvent.VK_C, menuMask, DefaultEditorKit.copyAction);
        bind(component, KeyEvent.VK_X, menuMask, DefaultEditorKit.cutAction);
        bind(component, KeyEvent.VK_A, menuMask, DefaultEditorKit.selectAllAction);
    }

    private static void bind(final @NotNull JTextComponent component, final int keyCode, final int modifiers, final @NotNull String actionName) {
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }
}
