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

package org.testin.search;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.Rows;
import org.testin.ui.framework.SelectionList;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextFieldWithSelections;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public final class GlobalSearchDialog extends AbstractFrameworkDialog<TextFieldWithSelections<Hit>> {
    // UC-INTERNAL-001
    public GlobalSearchDialog(final @NotNull Project p) {
        super(p);

        title = Bundle.message("dialog.search.title");

        components = List.of(
                ComponentDialogBase.<Hit>textFieldWithSelections()
                        .icon(AllIcons.Actions.Search)
                        .placeholder(Bundle.message("dialog.search.placeholder"))
                        .rows(query -> rowsFor(p, query))
                        .build());

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.search.shortcut.goto"), this::submit),
                StatusBarShortcut.select(),
                StatusBarShortcut.cancel(this::closeCancel)
        );

        preferredSize = JBUI.size(700, 460);

        // Rule-INTERNAL-076
        dismissOnClickOutside = true;
    }

    // UC-INTERNAL-001, Rule-INTERNAL-072, Rule-INTERNAL-073
    private static @NotNull Rows.Answer<Hit> rowsFor(final @NotNull Project p, final @NotNull String query) {
        final @NotNull Hits.Found found = Hits.forQuery(p, query);

        final @NotNull List<SelectionList<Hit>> rows = found.hits().stream()
                .map(hit -> SelectionList.add(hit.icon(), hit.name(), hit.where(), hit))
                .toList();

        if (query.isBlank() || found.matched() == 0) return Rows.Answer.of(rows);

        return new Rows.Answer<>(rows, Bundle.message("dialog.search.found", found.matched()));
    }

    // UC-INTERNAL-001, Rule-INTERNAL-002
    @Override
    protected void submit() {
        component().selection().ifPresent(hit -> {
            closeOk();
            GoTo.the(p, hit);
        });
    }
}
