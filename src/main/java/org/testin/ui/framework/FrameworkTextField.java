/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.ui.framework;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.TextComponentEmptyText;
import com.intellij.ui.components.fields.ExtendableTextField;
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
 * search field did that; rename, the commit message, and the Git name and email
 * did not. The multi-line area was the worst of them: it
 * replaces the paste action to insert a pasted screenshot and never bound the
 * key that reaches it, so its whole image-paste feature rested on a binding the
 * class next door documents as unreliable.
 */
final class FrameworkTextField {

    private final @NotNull ExtendableTextField field;
    private final @NotNull String placeholder;

    /**
     * What is drawn around the text right now. Held because the two are set
     * from different places at different times and applied together.
     */
    private @NotNull Icon icon;
    private @NotNull String note = "";

    private boolean emptyWarningShown;

    FrameworkTextField(final @NotNull Icon icon, final @NotNull String placeholder, final @NotNull String initialValue) {
        this.placeholder = placeholder;
        this.field = new ExtendableTextField(initialValue);
        this.icon = icon;
        DialogStyle.setDecorations(field, icon, note);

        style(field);

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

        bindClipboard(field);
    }

    /**
     * The framework field's look: its font and its padding. One owner for this
     * Swing field and the spell-checked editor field, so the two cannot drift
     * (#314).
     */
    static void style(final @NotNull JComponent field) {
        // Derived from the label font at construction, so every dialog open
        // picks up the current IDE font-size setting.
        field.setFont(JBFont.label().biggerOn(6f));
        // 12px left rhythm shared by the field text and any list rows below.
        field.setBorder(JBUI.Borders.empty(10, 12));
    }

    /**
     * The icon drawn before the text, which the field whose icon follows the
     * selection changes after construction. Here rather than at that caller,
     * because the icon and the note are drawn together, and this class holds
     * both.
     */
    void setLeadingIcon(final @NotNull Icon icon) {
        this.icon = icon;
        DialogStyle.setDecorations(field, icon, note);
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-073.
     * <p>
     * One short thing said at the end of the field - how many the search
     * matched - and empty to say nothing.
     */
    void setNote(final @NotNull String note) {
        this.note = note;
        DialogStyle.setDecorations(field, icon, note);
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
        bind(component, KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), DefaultEditorKit.pasteAction);
        bindAllButPaste(component);
    }

    /**
     * Cut, copy and select-all, for a component whose paste is answered
     * elsewhere - the area that takes a pasted screenshot, whose registered
     * action is the one handler for paste. Bound here as well, the area's own
     * Ctrl+V was dead behind that action, and a tester who moved Paste to
     * another key got image paste on it and plain text on Ctrl+V (#66, finding
     * 271).
     */
    static void bindAllButPaste(final @NotNull JTextComponent component) {
        final int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        bind(component, KeyEvent.VK_C, menuMask, DefaultEditorKit.copyAction);
        bind(component, KeyEvent.VK_X, menuMask, DefaultEditorKit.cutAction);
        bind(component, KeyEvent.VK_A, menuMask, DefaultEditorKit.selectAllAction);
    }

    private static void bind(final @NotNull JTextComponent component, final int keyCode, @MagicConstant(flagsFromClass = InputEvent.class) final int modifiers, final @NotNull String actionName) {
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }
}
