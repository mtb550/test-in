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

package org.testin.explorer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.creator.CreateTestProjectAction;
import org.testin.explorer.toolbar.CollapseAllAction;
import org.testin.explorer.toolbar.ExpandAllAction;
import org.testin.search.GlobalSearchAction;
import org.testin.setting.OpenSettingsAction;
import org.testin.testproject.SelectTestProjectAction;

import java.util.List;

public class TreePanelActions {

    // UC-TREE-PANEL-028
    public @NotNull List<AnAction> create(final @NotNull Project p, final @NotNull TreePanel tp) {
        return List.of(
                // The keystroke reaches the search from anywhere, which is the
                // point of it - and is also why nothing on screen says the
                // search exists. The button is where a tester finds out.
                GlobalSearchAction.registered(),
                new OpenSettingsAction(p),
                new ExpandAllAction(tp),
                new CollapseAllAction(tp),
                tp.getRefreshAction(),
                new SelectTestProjectAction(p, tp),
                new CreateTestProjectAction(p, tp)
        );
    }
}