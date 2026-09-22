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
import org.testin.model.Groups;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.TestEditorAttributes;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.SelectionTable;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class GroupSelectionDialog extends AbstractFrameworkDialog {
    private final @NotNull SelectionTable groups;
    private final @NotNull Consumer<@NotNull String> onPicked;

    public GroupSelectionDialog(final @NotNull Project p, final @NotNull String currentSelection, final @NotNull Consumer<@NotNull String> onPicked) {
        super(p);
        this.onPicked = onPicked;

        title = Bundle.message("dialog.groups.title");

        final @NotNull ComponentDialogBase<SelectionTable> table = ComponentDialogBase.table()
                .column(TestEditorAttributes.GROUP.getName(), 260)
                .build();

        components = List.of(table);

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.hint("Ctrl+Click", Bundle.message("shortcut.add")),
                StatusBarShortcut.cancel(this::closeCancel));

        groups = table.getComponent();

        groups.addRow(Groups.NONE);
        Services.getInstance(p, TestCaseValues.class).getGroups().stream().sorted().forEach(groups::addRow);
        groups.selectRows(rowsOf(currentSelection));
    }

    private @NotNull List<Integer> rowsOf(final @NotNull String currentSelection) {
        final @NotNull List<Integer> rows = new ArrayList<>();
        if (currentSelection.isBlank()) return rows;

        final @NotNull List<String> selected = Arrays.stream(currentSelection.split(","))
                .map(String::trim)
                .toList();

        for (int row = 0; row < groups.getRowCount(); row++) {
            if (selected.contains(groups.getValueAt(row, 0))) rows.add(row);
        }

        return rows;
    }

    // UC-SHARE-003
    @Override
    protected void submit() {
        onPicked.accept(selectedGroupsStr());
        closeOk();
    }

    private @NotNull String selectedGroupsStr() {
        return Groups.text(groups.getSelectedRows().stream()
                .map(row -> groups.getValueAt(row, 0))
                .toList());
    }
}
