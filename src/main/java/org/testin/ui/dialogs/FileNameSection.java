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

package org.testin.ui.dialogs;

import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.Caption;
import org.testin.ui.framework.EmptyWarning;
import org.testin.util.Bundle;

import javax.swing.JComponent;
import java.util.Optional;

public record FileNameSection(@NotNull JBTextField field) {
    private static final int COLUMNS = 30;

    // Rule-INTERNAL-095, Rule-INTERNAL-096
    public static @NotNull FileNameSection of(final @NotNull String fileName) {
        final @NotNull JBTextField field = new JBTextField(COLUMNS);
        DialogStyle.asField(field);
        // Rule-INTERNAL-096
        DialogStyle.framed(field);
        field.setText(fileName);

        return new FileNameSection(field);
    }

    // Rule-INTERNAL-087
    public @NotNull JComponent panel() {
        return Caption.above(Bundle.message("destination.caption.file"), field);
    }

    // UC-INTERNAL-007, Rule-INTERNAL-067
    public @NotNull Optional<String> accepted() {
        final @NotNull String fileName = field.getText().trim();
        if (!fileName.isEmpty()) return Optional.of(fileName);

        EmptyWarning.show(field, Bundle.message("destination.name.the.file"));
        return Optional.empty();
    }
}
