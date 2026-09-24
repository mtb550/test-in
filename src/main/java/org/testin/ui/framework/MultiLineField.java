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
import java.awt.Font;
import java.util.Optional;

// Rule-INTERNAL-096, Rule-INTERNAL-097
public final class MultiLineField implements DialogComponent {
    private final @NotNull Project p;
    private final @NotNull EditorTextField field;
    private final @NotNull String caption;

    private @NotNull Optional<JComponent> panel = Optional.empty();
    private @NotNull Optional<AbstractFrameworkDialog> host = Optional.empty();

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

        field.addSettingsProvider(editor -> {
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            editor.setBorder(new DarculaEditorTextFieldBorder(field, editor));

            final @NotNull EditorColorsScheme themed = editor.createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getSchemeForCurrentUITheme());
            final @NotNull Font font = Fonts.field();
            themed.setEditorFontName(font.getFontName());
            themed.setEditorFontSize(font.getSize());
            editor.setColorsScheme(themed);
        });
    }

    // Rule-INTERNAL-060, Rule-EDITOR-PANEL-048, Rule-EDITOR-PANEL-246
    public void enableMultiLine(final @NotNull AbstractFrameworkDialog base, final @NotNull Runnable onSave) {
        field.addSettingsProvider(editor -> base.registerShortcut(editor.getContentComponent(), Shortcuts.InsertNewLine.getCustomShortcut(), () -> insertNewLine(editor)));

        growsWith(base::refit);

        base.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), onSave);
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

    @Override
    public void hostedBy(final @NotNull AbstractFrameworkDialog base) {
        host = Optional.of(base);
    }

    // Rule-EDITOR-PANEL-246
    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        host.ifPresent(base -> enableMultiLine(base, submit));
    }

    // Rule-EDITOR-PANEL-246
    private void insertNewLine(final @NotNull Editor editor) {
        final int caret = editor.getCaretModel().getOffset();
        WriteCommandAction.runWriteCommandAction(p, () -> {
            editor.getDocument().insertString(caret, "\n");
            editor.getCaretModel().moveToOffset(caret + 1);
        });
    }

    // Rule-INTERNAL-097
    private void grew(final @NotNull Runnable refit) {
        final int height = field.getPreferredSize().height;
        if (height == packedHeight) return;

        packedHeight = height;
        refit.run();
    }
}
