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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.testcase.TestCaseSnapshot;
import org.testin.testcase.TestEditorAttributes;
import org.testin.testcase.TestEditorAttributes.Can;
import org.testin.undo.UndoScope;
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

public class GridEditListener extends AbstractGridEditListener {
    private final @NotNull Runnable onEdited;

    private final @NotNull Path testSetPath;

    private final @NotNull Map<UUID, Changed> changedThisGesture = new LinkedHashMap<>();

    public GridEditListener(final @NotNull Project p, final @NotNull List<TestCaseDto> pageItems, final @NotNull Runnable onEdited, final @NotNull Path testSetPath) {
        super(p, pageItems);
        this.onEdited = onEdited;
        this.testSetPath = testSetPath;
    }

    private static @NotNull String quoted(final @NotNull String typed, final @NotNull TestEditorAttributes attr) {
        return Bundle.message("grid.refused.as", typed.trim(), attr.getName());
    }

    @Override
    protected int columnCount() {
        return TestEditorAttributes.values().length;
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-050
    @Override
    protected @NotNull GridEdit apply(final @NotNull DefaultTableModel model, final @NotNull TestCaseDto tc, final int row, final int col) {
        final @NotNull TestEditorAttributes attr = TestEditorAttributes.values()[col];

        if (!attr.can(Can.EDIT)) return GridEdit.UNCHANGED;

        final @NotNull TestCaseSnapshot undoFrom = Optional.ofNullable(changedThisGesture.get(tc.getId()))
                .map(Changed::before)
                .orElseGet(() -> TestCaseSnapshot.of(p, testSetPath, List.of(tc.getId())));

        final @NotNull String typed = String.valueOf(model.getValueAt(row, col));

        final @NotNull Object before = attr.gridValue(tc);
        final boolean took = attr.getImportSetter().execute(p, tc, typed);
        final @NotNull Object after = attr.gridValue(tc);

        model.setValueAt(after, row, col);

        // Rule-EDITOR-PANEL-206
        if (!took) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.UNREADABLE, quoted(typed, attr));
            return GridEdit.REFUSED;
        }

        if (Objects.equals(before, after)) return GridEdit.UNCHANGED;

        persistAndGenerate(tc, attr, undoFrom);
        onEdited.run();

        return GridEdit.WROTE;
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053
    private void persistAndGenerate(final @NotNull TestCaseDto tc, final @NotNull TestEditorAttributes attr, final @NotNull TestCaseSnapshot undoFrom) {
        if (testSetPath.toString().isEmpty()) {
            Logger.warn("[grid] edit not persisted - the editor has no test set path");
            return;
        }

        final boolean first = changedThisGesture.isEmpty();
        changedThisGesture.computeIfAbsent(tc.getId(), _ -> new Changed(tc, undoFrom, new LinkedHashSet<>()))
                .generators().add(attr.getGenType());

        if (first) ApplicationManager.getApplication().invokeLater(this::saveTheGesture);
    }

    // UC-EDITOR-PANEL-008, Rule-EDITOR-PANEL-053, Rule-EDITOR-PANEL-038
    private void saveTheGesture() {
        final @NotNull List<Changed> gesture = List.copyOf(changedThisGesture.values());
        changedThisGesture.clear();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            final @NotNull List<TestCaseDto> written = new ArrayList<>();
            final @NotNull List<TestCaseSnapshot> before = new ArrayList<>();

            for (final Changed changed : gesture) {
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

    private record Changed(@NotNull TestCaseDto tc, @NotNull TestCaseSnapshot before, @NotNull Set<GenType> generators) {
    }
}
