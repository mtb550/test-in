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

package org.testin.importexport.shared;

import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

@RequiredArgsConstructor
public class CellEditListener implements TableModelListener {
    private final @NotNull List<TestEditorAttributes> importAttributes;
    private final @NotNull Project p;
    private final @NotNull List<TestCaseDto> testCases;
    private boolean isUpdating = false;

    // UC-SHARE-003, Rule-SHARE-021
    @Override
    public void tableChanged(final @NotNull TableModelEvent e) {
        if (isUpdating) return;

        if (e.getType() == TableModelEvent.UPDATE) {
            final int row = e.getFirstRow();
            final int col = e.getColumn();

            if (row >= 0 && col >= 2) {
                isUpdating = true;
                try {
                    final @NotNull DefaultTableModel model = (DefaultTableModel) e.getSource();
                    final @NotNull String updatedValue = String.valueOf(model.getValueAt(row, col));
                    final @NotNull TestEditorAttributes currentAttr = importAttributes.get(col - 2);
                    final @NotNull TestCaseDto tc = testCases.get(row);

                    // Rule-SHARE-106. The preview cell redraws with the
                    // old value, so a refused typo has to say so or it reads as
                    // an edit that did nothing (#264).
                    if (!currentAttr.getImportSetter().execute(p, tc, updatedValue)) {
                        TestEditorAttributes.sayWhatWasRefused(p, 1);
                    }

                    final @NotNull String formattedValue = currentAttr.gridValue(tc);
                    model.setValueAt(formattedValue, row, col);
                } finally {
                    isUpdating = false;
                }
            }
        }
    }
}
