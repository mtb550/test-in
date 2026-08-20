package org.testin.testcase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.editor.test.TestEditorContextMenu;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Shortcuts;

import java.util.*;

public class RemoveTestCaseAction extends AbstractProjectAction {
    private final @NotNull DirectoryDto dir;
    private final @NotNull TestinEditor editor;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;

    public RemoveTestCaseAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull DirectoryDto dir,
                                final @NotNull JBList<TestCaseDto> list,
                                final @NotNull CollectionListModel<TestCaseDto> model) {
        super(p, "Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.editor = editor;
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(Shortcuts.DeletePackage.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        final Runnable delete = () -> ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedItems));

        // A pending cut removes its source as the second half of a move the
        // tester already asked for, so it is not confirmed again.
        final boolean isCutAndSelected = TestEditorContextMenu.isGlobalCutAction() &&
                selectedItems.stream().allMatch(tc -> TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId()));

        if (isCutAndSelected) {
            delete.run();
            return;
        }

        final String msg = selectedItems.size() == 1
                ? "Remove '" + selectedItems.getFirst().getDescription() + "'?"
                : "Remove these " + selectedItems.size() + " test cases?";

        new ConfirmDialog(p, "Confirm Removing", msg, dir.getPath().toString(), null, "Remove", () -> {
            delete.run();

            // Inside the confirmation callback, not around actionPerformed: a
            // canceled dialog removes nothing and says nothing (#62).
            Services.getInstance(p, Notifier.class).softShowCounted(p, "Removed", selectedItems.size());
        }).show();
    }

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

        final var indexer = Services.getInstance(p, org.testin.indexer.ProjectIndexer.class);
        for (final TestCaseDto tc : selectedItems) {
            indexer.removeTestCase(dir.getPath(), tc.getId());
            GenType.REMOVE_TEST_CASE.getAction().execute(p, tc);
        }

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            model.remove(model.getElementIndex(selectedItems.get(i)));
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
