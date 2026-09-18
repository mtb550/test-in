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

import org.testin.undo.UndoScope;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.util.Bundle;

import javax.swing.table.DefaultTableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Writes a test grid edit into the test case, and regenerates the automation
 * code for the field that changed.
 * <p>
 * The guards, and saying the edit landed, are
 * {@link AbstractGridEditListener}'s - shared with the run grid, which needs
 * both for the same reasons and used to carry its own copy of them.
 */
public class GridEditListener extends AbstractGridEditListener {

    private final @NotNull Runnable onEdited;

    /**
     * The test set this grid belongs to; the grid never mixes test sets.
     */
    private final @NotNull Path testSetPath;

    /**
     * The test cases this gesture has changed, each with what it held before the
     * first of its cells changed and the generators its changed fields need.
     * Empty between gestures: the first cell schedules the save, and the rest of
     * the gesture adds itself to it - on the EDT, like every cell edit.
     */
    private final @NotNull Map<UUID, Changed> changedThisGesture = new LinkedHashMap<>();

    private record Changed(@NotNull TestCaseDto tc, @NotNull TestCaseSnapshot before, @NotNull Set<GenType> generators) {
    }

    public GridEditListener(final @NotNull Project p, final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited, final @NotNull Path testSetPath) {
        super(p, pageItems);
        this.onEdited = onEdited;
        this.testSetPath = testSetPath;
    }

    @Override
    protected int columnCount() {
        return TestEditorAttributes.values().length;
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-050
    @Override
    protected @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto tc, final int row, final int col) {
        final @NotNull TestEditorAttributes attr = TestEditorAttributes.values()[col];

        // The table model refuses these columns already; asked again of the same
        // attribute because a programmatic setValueAt never goes through the
        // model's answer.
        if (!attr.can(Can.EDIT)) return GridEdit.UNCHANGED;

        // Taken first of all: the setter below writes into the DTO the index is
        // holding, so a snapshot after it would be a snapshot of the edit. Only
        // for the first cell of this case in a gesture - for the rest, the case
        // as it was is the snapshot already held.
        final @NotNull TestCaseSnapshot undoFrom = Optional.ofNullable(changedThisGesture.get(tc.getId()))
                .map(Changed::before)
                .orElseGet(() -> TestCaseSnapshot.of(p, testSetPath, List.of(tc.getId())));

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));

        final @NotNull Object before = attr.gridValue(tc);
        final boolean took = attr.getImportSetter().execute(p, tc, typed);
        final @NotNull Object after = attr.gridValue(tc);

        // Always write the normalized value back to the cell - it renumbers
        // steps and drops blank entries even when nothing really changed. That
        // the cell may now differ from what was typed is said by the parent,
        // which both grids run through (#203).
        model.setValueAt(after, row, col);

        // Rule-EDITOR-PANEL-206. The cell redraws with the old value either way,
        // so without this a refused typo and an edit that changed nothing look
        // identical to the tester (#204). The parent is told this was a refusal,
        // so it does not say the same thing again in other words.
        if (!took) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.UNREADABLE, quoted(typed, attr));
            return GridEdit.REFUSED;
        }

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        persistAndGenerate(tc, attr, undoFrom);
        onEdited.run();

        return GridEdit.WROTE;
    }

    /**
     * What the refusal names: the text that could not be read, and the column it
     * was typed into. The column matters - a grid is eighteen of them, and
     * "Urgent" is refused by Priority and taken by Module.
     */
    private static @NotNull String quoted(final @NotNull String typed, final @NotNull TestEditorAttributes attr) {
        return Bundle.message("grid.refused.as", typed.trim(), attr.getName());
    }

    /**
     * UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053.
     * <p>
     * Adds this cell's case to the gesture's save, which the first cell of the
     * gesture schedules.
     */
    private void persistAndGenerate(final @NotNull TestCaseDto tc, final @NotNull TestEditorAttributes attr, final @NotNull TestCaseSnapshot undoFrom) {
        if (testSetPath.toString().isEmpty()) {
            Logger.warn("[grid] edit not persisted - the editor has no test set path");
            return;
        }

        final boolean first = changedThisGesture.isEmpty();
        changedThisGesture.computeIfAbsent(tc.getId(), id -> new Changed(tc, undoFrom, new LinkedHashSet<>()))
                .generators().add(attr.getGenType());

        // After the gesture's last cell, which is in this same event on the EDT.
        if (first) ApplicationManager.getApplication().invokeLater(this::saveTheGesture);
    }

    /**
     * UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053, Rule-EDITOR-PANEL-038.
     * <p>
     * Same behavior as the update dialog: write each test case the gesture
     * changed, update the generated code for the fields that changed, and put
     * the whole gesture on the undo history as one step. Off the EDT - the code
     * generators schedule their own write commands.
     * <p>
     * <b>Once per test case, not once per cell.</b> A cut or paste across a row
     * sets every cell in turn, and each cell used to be its own save and its own
     * undo entry. Ctrl+Z then took back one field, and the next press refused
     * with "These test cases changed since": each entry's after-snapshot was the
     * whole case, audit stamp included, and the other cells' saves had moved it
     * on - so the rest of the tester's text was on disk nowhere and in the undo
     * history unreachably (#66, finding 171).
     */
    private void saveTheGesture() {
        final @NotNull List<Changed> gesture = List.copyOf(changedThisGesture.values());
        changedThisGesture.clear();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final @NotNull List<TestCaseDto> written = new ArrayList<>();
            final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();

            for (final Changed changed : gesture) {
                // Only what reached disk is regenerated and put on the undo
                // history. A refused write was said by the writer, and a code
                // change or an undo entry for it would describe an edit that did
                // not happen (#66, finding 163).
                if (!indexer.putTestCase(testSetPath, changed.tc())) continue;

                changed.generators().forEach(generator -> generator.getAction().execute(p, changed.tc()));
                written.add(changed.tc());
                before.add(changed.before());
            }

            if (written.isEmpty()) return;

            TestCaseSnapshot.record(p, UndoScope.of(testSetPath), TestCaseSnapshot.describe(Bundle.message("snapshot.verb.edit"), written),
                    before, List.of(TestCaseSnapshot.of(p, testSetPath, TestCaseSnapshot.idsOf(written))));
        });
    }
}
