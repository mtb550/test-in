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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.util.Fonts;
import org.testin.util.Shortcuts;
import org.testin.util.SpellChecker;

import javax.swing.JComponent;
import java.awt.Dimension;

// UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-221, Rule-EDITOR-PANEL-246
public final class SpellCheckedArea implements DialogComponent {
    private static final int ROW_PADDING = 8;

    private final @NotNull EditorTextField field;
    private final @NotNull JBPanel<?> panel;

    SpellCheckedArea(final @NotNull Project p, final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value, final int rows) {
        field = SpellChecker.createField(p);
        field.setOneLineMode(false);
        field.setText(value);
        field.setPlaceholder(placeholder);
        field.setShowPlaceholderWhenFocused(true);
        FrameworkTextField.style(field);
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, rows * field.getFontMetrics(Fonts.field()).getHeight() + JBUI.scale(ROW_PADDING)));

        // UC-EDITOR-PANEL-034, Rule-INTERNAL-087
        panel = Caption.above(caption, field);
    }

    public @NotNull String getText() {
        return field.getText();
    }

    @Override
    public @NotNull JComponent getPanel() {
        return panel;
    }

    @Override
    public @NotNull JComponent getFocusComponent() {
        return field;
    }

    // UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-246
    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
        DumbAwareAction.create(_ -> submit.run()).registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), field);

        field.addSettingsProvider(editor -> DumbAwareAction.create((AnActionEvent _) -> EditorModificationUtil.insertStringAtCaret(editor, "\n"))
                .registerCustomShortcutSet(Shortcuts.InsertNewLine.getCustomShortcut(), editor.getContentComponent()));
    }
}
