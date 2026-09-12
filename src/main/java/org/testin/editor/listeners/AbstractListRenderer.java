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

import com.intellij.util.ui.JBUI;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.BaseCard;
import org.testin.editor.EditorColors;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;
import javax.swing.border.Border;

@AllArgsConstructor
public abstract class AbstractListRenderer<U extends TestinEditor> implements ListCellRenderer<TestCaseDto> {

    private static final @NotNull Border SELECTED_BORDER = JBUI.Borders.customLine(EditorColors.SELECTION_BORDER, 1);
    private static final @NotNull Border UNSELECTED_BORDER = JBUI.Borders.empty(1);
    protected final @NotNull U editor;

    // UC-EDITOR-PANEL-001, UC-EDITOR-PANEL-030
    @Override
    public @NotNull BaseCard getListCellRendererComponent(final JList<? extends TestCaseDto> list, final TestCaseDto tc, final int index, final boolean isSelected, final boolean cellHasFocus) {
        final boolean isRowHovered = (index == editor.getHoveredIndex());
        final @NotNull String hover = isRowHovered ? editor.getHoveredIconAction() : "";

        // The row, not the case's position in the set: this is what stripes the
        // cards, and stripes alternate down the screen. The number in the title
        // is the position, and the editor is asked for that by name.
        final @NotNull BaseCard card = bindDataAndGetCard(list, tc, index, isSelected, isRowHovered, hover);

        card.setBorder(isSelected ? SELECTED_BORDER : UNSELECTED_BORDER);

        return card;
    }

    protected abstract @NotNull BaseCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover);
}

