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

import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.util.SpellChecker;

import javax.swing.JComponent;

// UC-EDITOR-PANEL-034, Rule-EDITOR-PANEL-221
public final class SpellCheckedField implements DialogComponent {
    private final @NotNull EditorTextField field;
    private final @NotNull JBPanel<?> panel;

    SpellCheckedField(final @NotNull Project p, final @NotNull String caption, final @NotNull String placeholder, final @NotNull String value) {
        field = SpellChecker.createField(p);
        field.setOneLineMode(true);
        field.setText(value);
        field.setPlaceholder(placeholder);
        field.setShowPlaceholderWhenFocused(true);
        FrameworkTextField.style(field);

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

    @Override
    public void onSubmitRequest(final @NotNull Runnable submit) {
    }
}
