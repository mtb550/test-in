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

package org.testin.testcase;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// UC-EDITOR-PANEL-006
public class UpdateTestCaseAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-006
    public static void openField(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull UpdateTestCaseFields field) {
        new Work(p, editor).overSelection(menu -> menu.open(field));
    }

    // UC-EDITOR-PANEL-006
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.editor(e).ifPresent(editor -> new Work(p, editor).overSelection(TestCaseUpdateMenuDialog::show));
    }

    // UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194
    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            GrayWithReason.unless(this, e, false, Bundle.message("update.case.disabled.description"));
            return;
        }

        GrayWithReason.unless(this, e, TestinData.editor(e).isPresent() && !TestinData.selectedCases(e).isEmpty(), Bundle.message("action.select.case.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p, @NotNull TestinEditor editor) {
        private void overSelection(final @NotNull Consumer<TestCaseUpdateMenuDialog> open) {
            final @NotNull List<TestCaseDto> selectedItems = editor.getSelectedTestCases();
            if (selectedItems.isEmpty()) return;

            final @NotNull Path path = editor.getParent().getPath();

            Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

            final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, path, ids);

            open.accept(new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, gt) -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                int counted = 0;
                for (final TestCaseDto tc : updatedItems)
                    if (indexer.putTestCase(path, tc)) counted++;
                final int written = counted;

                if (written == 0) return;

                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), updatedItems), before, TestCaseSnapshot.of(p, path, ids));

                ApplicationManager.getApplication().invokeLater(() -> {
                    // Rule-EDITOR-PANEL-008
                    Services.getInstance(p, Notifier.class).softShowCounted(p,
                            gt == GenType.UPDATE_TEST_CASE_ORDER ? Done.RE_SORTED : Done.UPDATED, written);

                    if (editor instanceof Toolbar)
                        ((Toolbar) editor).onToolBarFilterSelectionChanged();

                    editor.refreshOrdered();
                    TestCaseUpdateMenuDialog.applyAftermath(p, updatedItems, gt);
                });
            })));
        }
    }
}
