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
import org.testin.editor.run.RunEditor;
import org.testin.testrun.RunEditorAttributes;
import org.testin.testrun.RunStatusService;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Writes a run grid edit into the run (#74).
 * <p>
 * The counterpart of {@link GridEditListener}, and deliberately not the same
 * class: a test case edit writes a test case and regenerates automation code, a
 * run edit writes the run JSON and generates nothing. What the two do share -
 * the guards, and confirming the edit - is {@link AbstractGridEditListener}'s.
 */
public class RunGridEditListener extends AbstractGridEditListener {

    private final @NotNull RunEditor editor;

    /**
     * Repaints the list behind the grid, so a card shows what was typed into the
     * cell when the tester switches back.
     */
    private final @NotNull Runnable onEdited;

    public RunGridEditListener(final @NotNull Project p, final @NotNull RunEditor editor, final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited) {
        super(p, pageItems);
        this.editor = editor;
        this.onEdited = onEdited;
    }

    @Override
    protected int columnCount() {
        return RunEditorAttributes.values().length;
    }

    // UC-EDITOR-PANEL-041, Rule-EDITOR-PANEL-174
    @Override
    protected @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto onThisRow, final int row, final int col) {
        final @NotNull RunEditorAttributes attr = RunEditorAttributes.values()[col];

        // The table model refuses these columns already; asked again of the same
        // attribute because a programmatic setValueAt never goes through the
        // model's answer.
        if (!attr.isEdited()) return GridEdit.UNCHANGED;

        final @NotNull Optional<TestRunItems> found = editor.runItem(onThisRow.getId());
        if (found.isEmpty()) return GridEdit.UNCHANGED;
        final @NotNull TestRunItems item = found.get();

        final @NotNull String before = attr.getRunValueExtractor().execute(item, p);

        // A run keeps what it recorded about a case that has since been deleted -
        // the same refusal the verdict path gives, worded once in the service.
        if (item.isRemoved()) {
            model.setValueAt(before, row, col);
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return GridEdit.REFUSED;
        }

        attr.getRunValueSetter().execute(item, String.valueOf(model.getValueAt(row, col)));
        final @NotNull String after = attr.getRunValueExtractor().execute(item, p);

        // Written back whatever happened: the value the run now holds is what the
        // cell must show, even where the setter normalized what was typed.
        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        Services.getInstance(p, RunStatusService.class).persistRun(p, editor);
        onEdited.run();

        return GridEdit.WROTE;
    }
}
