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

package org.testin.editor.toolbar.components;

import org.testin.editor.AbstractIconButton;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;

public class CreateTestCaseBtn extends AbstractIconButton implements ToolbarItem {

    // UC-EDITOR-PANEL-005
    public CreateTestCaseBtn(final @NotNull Runnable onToolBarCreateTestCaseClicked) {
        super("Create test case", AllIcons.General.Add, Declared.shortcutText("Testin.CreateTestCase"));

        addActionListener(e -> onToolBarCreateTestCaseClicked.run());
    }
}
