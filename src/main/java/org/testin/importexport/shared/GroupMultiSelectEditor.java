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

package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.Objects;

public class GroupMultiSelectEditor extends AbstractCellEditor implements TableCellEditor {
    private final @NotNull JButton button = new JButton();
    private @NotNull String currentValue = "";

    public GroupMultiSelectEditor(final @NotNull Project p) {
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(UIManager.getColor("Table.selectionBackground"));
        button.setForeground(UIManager.getColor("Table.selectionForeground"));

        // The dialog is a popup, not a modal: it answers through the callback
        // rather than on the line that showed it, and the cell stops editing
        // whichever way it closed - picked or canceled.
        button.addActionListener(e -> {
            final @NotNull GroupSelectionDialog dialog = new GroupSelectionDialog(p, currentValue, picked -> currentValue = picked);
            // A picker already open for another cell is raised instead, and this
            // one never built its popup - asking it when it closed threw at the
            // tester and left this cell stuck editing (#66, finding 210).
            if (dialog.show()) dialog.onClosed(this::fireEditingStopped);
            else fireEditingStopped();
        });
    }

    @Override
    public @NotNull Component getTableCellEditorComponent(final @NotNull JTable table, final @Nullable Object value, final boolean isSelected, final int row, final int column) {
        currentValue = Objects.toString(value, "");
        button.setText(currentValue);
        return button;
    }

    @Override
    public @NotNull Object getCellEditorValue() {
        return currentValue;
    }
}
