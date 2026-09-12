package org.testin.ui.framework;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.dialogs.DialogStyle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;

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

    private final @NotNull JTextField field;
    private final @NotNull String placeholder;

    /**
     * How this field takes what is drawn around its text, decided once by which
     * kind it is - a password field carries nothing.
     */
    private final @NotNull BiConsumer<@NotNull Icon, @NotNull String> decorations;

    /**
     * What is drawn around the text right now. Held because the two are set
     * from different places at different times and applied together.
     */
    private @NotNull Icon icon;
    private @NotNull String note = "";

    private boolean emptyWarningShown;

    /**
     * @param secret whether what the tester types is shown as dots. A server
     *               password is typed into a dialog somebody may be projecting,
     *               screen-sharing or recording, so the field that takes one
     *               does not echo it (#66, finding 64)
     */
    FrameworkTextField(final @NotNull Icon icon, final @NotNull String placeholder, final @NotNull String initialValue, final boolean secret) {
        this.placeholder = placeholder;

        if (secret) {
            // A password field carries no extensions, so there is no icon to
            // put in front of it - now or when a caller asks later. A secret
            // has nothing worth saying beside it anyway.
            final @NotNull JBPasswordField dots = new JBPasswordField();
            dots.setText(initialValue);

            this.field = dots;
            this.decorations = (ignoredIcon, ignoredNote) -> {
            };
        } else {
            final @NotNull ExtendableTextField plain = new ExtendableTextField(initialValue);

            this.field = plain;
            this.decorations = (wanted, saying) -> DialogStyle.setDecorations(plain, wanted, saying);
        }

        this.icon = icon;
        decorations.accept(icon, note);

        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        field.setFont(JBFont.label().biggerOn(6f));
        // 12px left rhythm shared by the field text and any list rows below.
        field.setBorder(JBUI.Borders.empty(10, 12));

        if (!placeholder.isBlank()) {
            emptyText().setText(placeholder);
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

        bindClipboard(field);
    }

    /**
     * The icon drawn before the text, which the field whose icon follows the
     * selection changes after construction. Here rather than at that caller,
     * because which kind of field this is - and therefore whether it can carry
     * an icon at all - is this class's own knowledge.
     */
    void setLeadingIcon(final @NotNull Icon icon) {
        this.icon = icon;
        decorations.accept(icon, note);
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-073.
     * <p>
     * One short thing said at the end of the field - how many the search
     * matched - and empty to say nothing.
     */
    void setNote(final @NotNull String note) {
        this.note = note;
        decorations.accept(icon, note);
    }

    @NotNull JTextField component() {
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

        // Through the shared one, so the framework dialogs and the report and
        // export dialog cannot drift into two ways of saying it (#251). The
        // placeholder is this field's own words, which is what it passes.
        EmptyWarning.show(field, placeholder);
    }

    private void showPlaceholder(final @NotNull SimpleTextAttributes attributes) {
        if (placeholder.isBlank()) return;

        emptyText().clear();
        emptyText().appendText(placeholder, attributes);
        field.repaint();
    }

    /**
     * The placeholder line of whichever field this is. Both kinds draw one, and
     * asking here is what lets the rest of this class be written once.
     */
    private @NotNull StatusText emptyText() {
        return ((ComponentWithEmptyText) field).getEmptyText();
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

    private static void bind(final @NotNull JTextComponent component, final int keyCode, @MagicConstant(flagsFromClass = InputEvent.class) final int modifiers, final @NotNull String actionName) {
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }
}
