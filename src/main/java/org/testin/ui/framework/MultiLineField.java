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

import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.dialogs.DialogStyle;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Optional;

// Rule-INTERNAL-096, Rule-INTERNAL-097
public final class MultiLineField implements DialogComponent {
    private static final int VISIBLE_LINES = 6;

    private final @NotNull Project p;
    private final @NotNull EditorTextField field;
    private final @NotNull String caption;

    private @NotNull Optional<JComponent> panel = Optional.empty();

    private int packedHeight;

    // Rule-INTERNAL-058, Rule-INTERNAL-095, Rule-INTERNAL-096
    public MultiLineField(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull String caption, final @NotNull String placeholder) {
        this.p = p;
        this.field = field;
        this.caption = caption;

        DialogStyle.asField(field);
        field.setPlaceholder(placeholder);
        field.setShowPlaceholderWhenFocused(true);
        field.setOneLineMode(false);

        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                capToVisibleLines();
            }
        });

        field.addSettingsProvider(editor -> {
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            editor.setBorder(new DarculaEditorTextFieldBorder(field, editor));
            // Rule-INTERNAL-102
            editor.setVerticalScrollbarVisible(true);
            // Rule-INTERNAL-103
            editor.getSettings().setShowIntentionBulb(false);

            final @NotNull EditorColorsScheme themed = editor.createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getSchemeForCurrentUITheme());
            final @NotNull Font font = Fonts.field();
            themed.setEditorFontName(font.getFontName());
            themed.setEditorFontSize(font.getSize());
            editor.setColorsScheme(themed);
        });
    }

    // Rule-INTERNAL-060, Rule-EDITOR-PANEL-048, Rule-EDITOR-PANEL-246
    public void enableMultiLine(final @NotNull DialogHost host, final @NotNull Runnable onSave) {
        insertsNewLine(host);
        growsWith(host::refit);

        host.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), onSave);
    }

    // Rule-TREE-PANEL-122
    public void ignoresEnter(final @NotNull DialogHost host) {
        host.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), () -> {
        });
    }

    // Rule-EDITOR-PANEL-048
    public void insertsNewLine(final @NotNull DialogHost host) {
        field.addSettingsProvider(editor -> host.registerShortcut(editor.getContentComponent(), Shortcuts.InsertNewLine.getCustomShortcut(), () -> insertNewLine(editor)));
    }

    // Rule-INTERNAL-097
    public void growsWith(final @NotNull Runnable refit) {
        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> grew(refit));
            }
        });
    }

    public @NotNull String getText() {
        return field.getText();
    }

    public void setText(final @NotNull String text) {
        field.setText(text);
    }

    public void setEnabled(final boolean enabled) {
        field.setEnabled(enabled);
    }

    // Rule-INTERNAL-087
    @Override
    public @NotNull JComponent getPanel() {
        if (panel.isEmpty()) panel = Optional.of(Caption.above(caption, field));

        return panel.orElseThrow();
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    // Rule-EDITOR-PANEL-246
    @Override
    public void hostedBy(final @NotNull DialogHost host, final @NotNull Runnable submit) {
        enableMultiLine(host, submit);
    }

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }

    // Rule-EDITOR-PANEL-246
    private void insertNewLine(final @NotNull Editor editor) {
        final int caret = editor.getCaretModel().getOffset();
        WriteCommandAction.runWriteCommandAction(p, () -> {
            editor.getDocument().insertString(caret, "\n");
            editor.getCaretModel().moveToOffset(caret + 1);
        });
    }

    // Rule-INTERNAL-102
    private void capToVisibleLines() {
        field.setPreferredSize(null);

        final @NotNull Dimension natural = field.getPreferredSize();
        final int cap = field.getFontMetrics(Fonts.field()).getHeight() * VISIBLE_LINES;
        if (natural.height <= cap) return;

        field.setPreferredSize(new Dimension(natural.width, cap));
        field.revalidate();
    }

    // Rule-INTERNAL-097
    private void grew(final @NotNull Runnable refit) {
        final int height = field.getPreferredSize().height;
        if (height == packedHeight) return;

        packedHeight = height;
        refit.run();
    }
}
