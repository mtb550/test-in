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

import org.testin.actions.GrayWithReason;
import org.testin.editor.TestinEditors;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.clipboard.CutState;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;

import java.util.*;
public class RemoveTestCaseAction extends DumbAwareAction {

    // UC-EDITOR-PANEL-011
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        final @NotNull List<TestCaseDto> selected = TestinData.selectedCases(e);
        if (p == null || selected.isEmpty()) return;

        TestinData.editor(e).ifPresent(editor -> new Work(p, editor, editor.getParent(), selected).remove());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Rule-EDITOR-PANEL-214. Asked of the node rather than of the editor's
        // type: a test run is not a test case container, which is the same flag
        // Import and Export read, so a node kind added later answers without
        // this method learning about it.
        if (TestinData.editor(e).filter(editor -> !editor.getParent().isTestCaseContainer()).isPresent()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setDescription(Bundle.message("remove.case.disabled.description"));
            return;
        }

        GrayWithReason.unless(this, e, !TestinData.selectedCases(e).isEmpty(), Bundle.message("action.select.case.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Removing one selection, for a project and an editor that are there.
     */
    private record Work(@NotNull Project p, @NotNull TestinEditor editor, @NotNull DirectoryDto dir, @NotNull List<TestCaseDto> selected) {

        // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-062
        void remove() {
                final @NotNull List<TestCaseDto> selectedItems = selected;
            if (selectedItems.isEmpty()) return;

            final @NotNull Runnable delete = () -> ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedItems));

            // Delete is Delete, whatever is on the clipboard. This used to skip
            // both the confirmation and the "Removed" balloon when every selected
            // row was pending a cut, on the grounds that a paste removes the
            // source as the second half of a move the tester already asked for -
            // but a paste has not gone through here since PasteTestCaseNodeAction
            // took over removing its own source rows. What was left was the
            // tester pressing Delete on rows they had cut: the rows vanished,
            // nothing was asked, nothing was said, and the cut stayed pending on
            // ids that no longer exist (#66, finding 81).

            final @NotNull String msg = selectedItems.size() == 1
                    ? Bundle.message("remove.case.confirm.one", selectedItems.getFirst().getDescription())
                    : Bundle.message("remove.case.confirm.many", String.valueOf(selectedItems.size()));

            // Confirmed from inside the deletion, once the files have answered,
            // and never around actionPerformed: a canceled dialog removes nothing
            // and says nothing (#62).
            new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, dir.getPath().toString(), "", Bundle.message("remove.confirm.button"), delete).show();
        }

        // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
        private void performDeletion(final @NotNull List<TestCaseDto> selectedItems) {
            // Nothing is relinked. A case carries its own position, so removing one
            // leaves a gap in the ranks and no case anywhere pointing at it - which
            // used to be a walk over the whole set rewriting the survivors on either
            // side of every removed run.
            //
            // Off the editor's master list first. The list model holds only the
            // current page, while the next sequence write persists every entry of
            // the master list.
            // So a case left there is written back to disk after its file has been
            // deleted, and comes back on the next re-index as an unsorted orphan.
            editor.getAllTestCases().removeAll(selectedItems);

            // Whatever route removed these rows, a cut waiting to paste them is
            // waiting for ids that have gone: the cards would draw faded for a
            // move that can never land.
            Services.getInstance(p, CutState.class).clear();

            // The files, and the snapshots either side of them, on a pooled thread,
            // the way a grid edit writes. On the EDT, removing forty cases did
            // forty deletes - each through the recycle bin - and two forty-case
            // snapshots before the key returned (#66, finding 224).
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                // The whole case, before it goes: its content, its id and its
                // rank, which is what puts it back where it was rather than at
                // the end of the set. Read from the index, which the editor's
                // list above does not change.
                final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
                final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, dir.getPath(), ids);

                // Only the cases whose files went are removed, and only they are
                // counted, lose their methods and are named in the undo. A file
                // the system would not delete keeps its case, and the writer has
                // said why. The balloon used to be raised before these deletes
                // had even started, for the whole selection (#66, finding 292).
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                final @NotNull List<TestCaseDto> removed = new ArrayList<>();
                for (final TestCaseDto tc : selectedItems) {
                    if (indexer.removeTestCase(dir.getPath(), tc.getId())) removed.add(tc);
                }

                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.remove"), removed), before, TestCaseSnapshot.of(p, dir.getPath(), ids));

                ApplicationManager.getApplication().invokeLater(() -> {
                    // One call for the whole selection. Only executeAll opens the
                    // single write command, so removing forty cases a case at a time
                    // was forty entries on the IDE's own undo history - the defect
                    // the batching in #153 was written to fix (#66, finding 80).
                    if (!removed.isEmpty()) GenType.REMOVE_TEST_CASE.executeAll(p, removed);

                    // Redrawn from the master list rather than by taking rows out
                    // of the page's model, which a declared action has no way to
                    // reach. It is also the more honest redraw: a page of fifty that
                    // loses three refills from the next page instead of standing at
                    // forty-seven. A case that stayed was taken off that list above,
                    // so then the editor reads its set again, and it comes back.
                    if (removed.size() == selectedItems.size()) editor.refreshView();
                    else Services.getInstance(p, TestinEditors.class).reloadOpen(p, dir.getPath());

                    if (!removed.isEmpty()) Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, removed.size());
                });
            });
        }


    }
}
