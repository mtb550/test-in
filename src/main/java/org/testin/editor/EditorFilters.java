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

package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.model.Automated;
import org.testin.model.Priority;

import java.util.Set;

/**
 * What the toolbar is currently narrowing the list by, read in one go.
 * <p>
 * Both editors opened their filtering with the same seven lines: the search
 * query, then the filter popup, then the three sets out of it.
 * <p>
 * Run status is deliberately not here. Only the run editor filters by it,
 * because only a run has one, and a field that half the callers must ignore is
 * worse than the caller that needs it asking for it.
 * <p>
 * Automation is here, because both editors show test cases and a test case
 * either has a generated method or does not, whichever list it is in.
 */
public record EditorFilters(@NotNull String query, @NotNull Set<String> groups, @NotNull Set<Priority> priorities, @NotNull Set<String> modules, @NotNull Set<Automated> automation) {

    public static @NotNull EditorFilters of(final @NotNull AbstractToolbarPanel toolBar) {
        final @NotNull FilterPopupBtn filters = toolBar.getToolbarItem(FilterPopupBtn.class);

        return new EditorFilters(
                toolBar.getSearchTxt().getSearchQuery(),
                filters.getSelectedGroup(),
                filters.getSelectedPriority(),
                filters.getSelectedModule(),
                filters.getSelectedAutomation());
    }
}
