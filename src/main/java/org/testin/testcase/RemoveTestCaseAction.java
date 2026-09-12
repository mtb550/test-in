package org.testin.testcase;

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

        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Removing one selection, for a project and an editor that are there.
     */
    private record Work(@NotNull Project p, @NotNull TestinEditor editor, @NotNull DirectoryDto dir, @NotNull List<TestCaseDto> selected) {

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

            new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, dir.getPath().toString(), "", Bundle.message("remove.confirm.button"), () -> {
                delete.run();

                // Inside the confirmation callback, not around actionPerformed: a
                // canceled dialog removes nothing and says nothing (#62).
                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, selectedItems.size());
            }).show();
        }

        // UC-EDITOR-PANEL-011, Rule-EDITOR-PANEL-064
        private void performDeletion(final @NotNull List<TestCaseDto> selectedItems) {
            // The whole case, before it goes: its content, its id and its rank, which
            // is what puts it back where it was rather than at the end of the set.
            final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, dir.getPath(), ids);

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

            final var indexer = Services.getInstance(p, org.testin.indexer.ProjectIndexer.class);
            for (final TestCaseDto tc : selectedItems) {
                indexer.removeTestCase(dir.getPath(), tc.getId());
            }

            // One call for the whole selection. Only executeAll opens the single
            // write command, so removing forty cases a case at a time was forty
            // entries on the IDE's own undo history - the defect the batching in
            // #153 was written to fix (#66, finding 80).
            GenType.REMOVE_TEST_CASE.executeAll(p, selectedItems);

            // Redrawn from the master list rather than by taking rows out of the
            // page's model, which a declared action has no way to reach. It is also
            // the more honest redraw: a page of fifty that loses three refills from
            // the next page instead of standing at forty-seven.
            editor.refreshView();

            TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.remove"), selectedItems), before, TestCaseSnapshot.of(p, dir.getPath(), ids));
        }


    }
}
