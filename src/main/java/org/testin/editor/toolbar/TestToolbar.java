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

package org.testin.editor.toolbar;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.CreateTestCaseBtn;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.GridViewBtn;
import org.testin.editor.toolbar.components.ListViewBtn;
import org.testin.editor.toolbar.components.RefreshBtn;
import org.testin.editor.toolbar.components.TestDetailsPopupBtn;
import org.testin.editor.toolbar.components.ToolbarItem;

import java.util.List;

public class TestToolbar extends AbstractToolbarPanel {

    public TestToolbar(final @NotNull Toolbar callbacks) {
        super(callbacks);
        layoutComponents();
    }

    // UC-EDITOR-PANEL-001
    @Override
    public @NotNull List<ToolbarItem> getCustomComponents() {
        return List.of(
                new CreateTestCaseBtn(getCallbacks()::onToolBarCreateTestCaseClicked),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new TestDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules, getCallbacks()::getAvailableGroups),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // The search field is created and laid out by AbstractToolbarPanel itself
                // because it needs its own horizontal-fill constraints.
        );
    }
}