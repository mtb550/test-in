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

package org.testin.editor.test;

import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.testin.actions.Declared;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.EscapeAction;
import org.testin.editor.AbstractEditorContextMenu;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.open.OpenContextMenuAction;
import org.testin.testcase.UpdateTestCaseAction;
import org.testin.testcase.UpdateTestCaseFields;

public class TestEditorContextMenu extends AbstractEditorContextMenu {
    private final @NotNull Project p;

    private final @NotNull TestinEditor ui;

    public TestEditorContextMenu(final @NotNull Project p, final @NotNull TestinEditor ui, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super();
        this.p = p;
        this.ui = ui;

        add(Declared.forMenu("Testin.CreateTestCase"));
        add(Declared.forMenu("Testin.ViewDetails"));
        add(Declared.forMenu("Testin.NavigateToTestCase"));

        addSeparator();

        add(Declared.forMenu("Testin.UpdateTestCase"));

        add(actions(p, dir, list));

        addSeparator();

        add(Declared.forMenu("Testin.AutomateTestCase"));
        add(Declared.forMenu("Testin.RunTestCase"));
        add(Declared.forMenu("Testin.NavigateToCode"));
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    public void registerShortcuts(final @NotNull JBList<TestCaseDto> list) {
        new EscapeAction(p, list);
        new OpenContextMenuAction(list, this);

        for (final UpdateTestCaseFields field : UpdateTestCaseFields.values()) {
            field.bindShortcut(list, () -> UpdateTestCaseAction.openField(p, ui, field));
        }
    }
}