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

package org.testin.testcase.create;

import com.intellij.ide.ui.laf.darcula.ui.DarculaEditorTextFieldBorder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.CreateTestCaseFields;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.awt.Font;

public abstract class AbstractMultiLineSection implements CreateTestCaseSection {
    protected final @NotNull Project p;

    protected final @NotNull EditorTextField field;

    private final @NotNull JBPanel<?> wrapper;

    private int packedHeight;

    protected AbstractMultiLineSection(final @NotNull Project p, final @NotNull EditorTextField field, final @NotNull CreateTestCaseFields describes) {
        this.p = p;
        this.field = field;
        styleField(this.field, describes);

        this.wrapper = createWrapper(describes.getIcon(), this.field);
    }

    public void enableMultiLine(final @NotNull TestCaseBaseDialog base, final @NotNull Runnable onSave) {
        field.setOneLineMode(false);

        field.addSettingsProvider(editor -> {
            editor.getContentComponent().setFocusTraversalKeysEnabled(true);

            base.registerShortcut(editor.getContentComponent(), Shortcuts.InsertNewLine.getCustomShortcut(), () -> EditorModificationUtil.insertStringAtCaret(editor, "\n"));

            editor.setBorder(new DarculaEditorTextFieldBorder(field, editor));

            final @NotNull EditorColorsScheme themed = editor.createBoundColorSchemeDelegate(EditorColorsManager.getInstance().getSchemeForCurrentUITheme());
            final @NotNull Font font = Fonts.field();
            themed.setEditorFontName(font.getFontName());
            themed.setEditorFontSize(font.getSize());
            editor.setColorsScheme(themed);
        });

        field.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(final @NotNull DocumentEvent event) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final int height = field.getPreferredSize().height;
                    if (height == packedHeight) return;

                    packedHeight = height;
                    base.refit();
                });
            }
        });

        base.registerShortcut(field, Shortcuts.Enter.getCustomShortcut(), onSave);
    }

    @Override
    public @NotNull JBPanel<?> getWrapper() {
        return wrapper;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    @Override
    public void setEditable(final boolean editable) {
        field.setEnabled(editable);
    }
}
