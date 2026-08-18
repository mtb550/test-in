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
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Shortcuts;
import org.testin.util.Tools;

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
        // The same pair the tree's Remove answers to, so the key means "remove"
        // wherever the tester is standing.
        this.registerCustomShortcutSet(
                Tools.customShortcut(Shortcuts.DeletePackage.getKey(), Shortcuts.RemoveNode.getKey()), list);
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
            Services.getInstance(p, Notifier.class).softShowCounted(p, "Test case", "removed", selectedItems.size());
        }).show();
    }

    private void performDeletion(final @NotNull List<TestCaseDto> selectedItems) {
        relinkAroundRemoved(selectedItems);

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

    /**
     * Stitches the linked list past every removed node. Handles non-contiguous
     * selections: each removed run is bridged by its surrounding survivors —
     * relinking only around first/last selected would leave middle survivors
     * unreachable and silently break the ordering.
     * <p>
     * Walks the editor's master list rather than the page model. The model holds
     * one page, so deleting the first case on page two would look like deleting
     * the head - page one's last case would keep pointing at a removed id.
     */
    private void relinkAroundRemoved(final @NotNull List<TestCaseDto> selectedItems) {
        final Set<UUID> removedIds = new HashSet<>();
        for (final TestCaseDto tc : selectedItems) removedIds.add(tc.getId());

        final List<TestCaseDto> source = editor.getAllTestCases();
        final List<TestCaseDto> all;
        synchronized (source) {
            all = new ArrayList<>(source);
        }

        TestCaseDto prevSurvivor = null;
        int i = 0;
        while (i < all.size()) {
            final TestCaseDto tc = all.get(i);
            if (!removedIds.contains(tc.getId())) {
                prevSurvivor = tc;
                i++;
                continue;
            }

            // A run of removed rows: find the survivor after it.
            int j = i;
            while (j < all.size() && removedIds.contains(all.get(j).getId())) j++;
            final TestCaseDto nextSurvivor = j < all.size() ? all.get(j) : null;

            if (prevSurvivor == null) {
                if (Boolean.TRUE.equals(tc.getIsHead()) && nextSurvivor != null) {
                    nextSurvivor.setIsHead(true);
                    saveToFile(nextSurvivor);
                }
            } else {
                prevSurvivor.setNext(nextSurvivor != null ? nextSurvivor.getId() : null);
                saveToFile(prevSurvivor);
            }
            i = j;
        }
    }

    private void saveToFile(final @NotNull TestCaseDto item) {
        Services.getInstance(p, ProjectIndexer.class).putTestCase(dir.getPath(), item);
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
