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
import org.testin.indexer.ProjectIndexer;
import org.testin.testrun.RunEditorAttributes;
import org.testin.testrun.RunStatusService;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class RunGridEditListener extends AbstractGridEditListener {
    private final @NotNull RunEditor editor;

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

        if (!attr.isEdited()) return GridEdit.UNCHANGED;

        final @NotNull Optional<TestRunItems> found = editor.runItem(onThisRow.getId());
        if (found.isEmpty()) return GridEdit.UNCHANGED;
        final @NotNull TestRunItems item = found.get();

        final @NotNull String before = attr.getRunValueExtractor().execute(item, p);

        if (item.isRemoved()) {
            model.setValueAt(before, row, col);
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return GridEdit.REFUSED;
        }

        // Rule-EDITOR-PANEL-174
        if (Services.getInstance(p, RunStatusService.class).heldRun(p, editor.getParent().getPath()).isEmpty()) {
            model.setValueAt(before, row, col);
            return GridEdit.REFUSED;
        }

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));
        attr.getRunValueSetter().execute(item, typed);
        final @NotNull String after = attr.getRunValueExtractor().execute(item, p);

        model.setValueAt(after, row, col);

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        Services.getInstance(p, ProjectIndexer.class).changeRun(editor.getParent().getPath(),
                run -> run.resultOf(onThisRow.getId()).ifPresent(result -> attr.getRunValueSetter().execute(result, typed)));
        onEdited.run();

        return GridEdit.WROTE;
    }
}
