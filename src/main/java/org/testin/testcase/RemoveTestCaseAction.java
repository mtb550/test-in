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
import org.testin.clipboard.CutState;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.editor.TestinEditors;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RemoveTestCaseAction extends DumbAwareAction {
    // UC-EDITOR-PANEL-011
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull List<TestCaseDto> selected = TestinData.selectedTestCases(e);
        if (p == null || selected.isEmpty()) return;

        TestinData.editor(e).ifPresent(editor -> new Work(p, editor, editor.getParent(), selected).remove());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("remove.case.disabled.description"));
            return;
        }

        GrayWithReason.unless(this, e, !TestinData.selectedTestCases(e).isEmpty(), Bundle.message("action.select.case.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p, @NotNull TestinEditor editor, @NotNull DirectoryDto dir, @NotNull List<TestCaseDto> selected) {
        // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-062
        void remove() {
            final @NotNull List<TestCaseDto> selectedItems = selected;
            if (selectedItems.isEmpty()) return;

            final @NotNull Runnable delete = () -> ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedItems));

            final @NotNull String msg = selectedItems.size() == 1
                    ? Bundle.message("remove.case.confirm.one", selectedItems.getFirst().getDescription())
                    : Bundle.message("remove.case.confirm.many", String.valueOf(selectedItems.size()));

            new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, dir.getPath().toString(), "", Bundle.message("remove.confirm.button"), delete).show();
        }

        // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
        private void performDeletion(final @NotNull List<TestCaseDto> selectedItems) {
            editor.getAllTestCases().removeAll(selectedItems);

            Services.getInstance(p, CutState.class).clear();

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
                final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, dir.getPath(), ids);

                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final @NotNull List<TestCaseDto> removed = new ArrayList<>();
                for (final TestCaseDto tc : selectedItems) {
                    if (indexer.removeTestCase(dir.getPath(), tc.getId())) removed.add(tc);
                }

                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.remove"), removed), before, TestCaseSnapshot.of(p, dir.getPath(), ids));

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!removed.isEmpty()) GenType.REMOVE_TEST_CASE.executeAll(p, removed);

                    if (removed.size() == selectedItems.size()) editor.refreshView();
                    else Services.getInstance(p, TestinEditors.class).reloadOpen(p, dir.getPath());

                    if (!removed.isEmpty())
                        Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, removed.size());
                });
            });
        }
    }
}
