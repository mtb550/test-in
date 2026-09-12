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

package org.testin.creator.dialogs;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Creates a test node (test set or test set package) under the test cases
 * main directory or a test set package. The constructor is the declaration:
 * title, components, status bar mapping — plus the submit action.
 */
public final class CreateTestDialog extends AbstractFrameworkDialog<TextFieldWithSelections<DirectoryType>> {

    private final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate;

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, Rule-TREE-PANEL-024
    public CreateTestDialog(final @NotNull Project p, final @NotNull BiConsumer<@NotNull String, @NotNull DirectoryType> onCreate) {
        super(p);
        this.onCreate = onCreate;

        title = Bundle.message("dialog.create.test.title");

        components = List.of(
                ComponentDialogBase.<DirectoryType>textFieldWithSelections()
                        .icon(DirectoryType.TS.getIcon())
                        .placeholder(Bundle.message("dialog.create.test.placeholder"))
                        .selection(DirectoryType.TS.getIcon(), DirectoryType.TS.getDescription(), Bundle.message("dialog.create.test.hint.ts"), DirectoryType.TS)
                        .selection(DirectoryType.TSP.getIcon(), DirectoryType.TSP.getDescription(), Bundle.message("dialog.create.test.hint.tsp"), DirectoryType.TSP)
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.confirm(this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel)
        );
    }

    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, Rule-TREE-PANEL-005, Rule-TREE-PANEL-095
    @Override
    protected void submit() {
        // Which of the two was picked decides what the name has to be able to
        // do: a test set package becomes a Java package and a test set becomes
        // the class. Asked of the type rather than written out here (#11).
        final @NotNull DirectoryType type = component().getSelectedValue();

        final @NotNull String name = accepted(component(), type::canTakeName, Refused.NOT_A_JAVA_NAME);
        if (name.isEmpty()) return;

        onCreate.accept(name, type);
        closeOk();
    }
}
