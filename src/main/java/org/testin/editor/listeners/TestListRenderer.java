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
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestCard;
import org.testin.editor.test.TestEditor;
import org.testin.model.dto.TestCaseDto;

import javax.swing.*;

public class TestListRenderer extends AbstractListRenderer<TestinEditor> {
    private final @NotNull TestCard card;

    public TestListRenderer(final @NotNull Project p, final @NotNull TestEditor editor) {
        super(editor);
        this.card = new TestCard(p);
    }

    // UC-EDITOR-PANEL-001
    @Override
    protected @NotNull TestCard bindDataAndGetCard(final @NotNull JList<? extends TestCaseDto> list, final @NotNull TestCaseDto tc, final int row, final boolean isSelected, final boolean isRowHovered, final @NotNull String hover) {

        card.updateData(row, tc, editor.getSelectedDetails(), editor.cardTitle(tc));
        card.setActionsState(isSelected, isRowHovered, hover);
        card.applyListLayout(list);

        return card;
    }
}