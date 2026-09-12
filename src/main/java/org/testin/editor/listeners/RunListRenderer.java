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

package org.testin.editor.listeners;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.run.RunCard;
import org.testin.editor.run.RunEditor;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;

public class RunListRenderer extends AbstractListRenderer<RunEditor> {
    private final @NotNull RunCard card;

    public RunListRenderer(final @NotNull Project p, final @NotNull RunEditor editor) {
        super(editor);
        this.card = new RunCard(p);
    }

    // UC-EDITOR-PANEL-030
    @Override
    protected @NotNull RunCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {
        // The results map can be transiently empty during a refresh while the list
        // still repaints; render a pending placeholder rather than crashing inside
        // the cell renderer.
        final @NotNull TestRunItems runItem = editor.runItem(tc.getId())
                .orElseGet(() -> TestRunItems.builder().id(tc.getId()).tc(tc).build());

        card.updateData(row, editor.getSelectedDetails(), runItem, editor.cardTitle(tc));
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListLayout(list);

        return card;
    }
}