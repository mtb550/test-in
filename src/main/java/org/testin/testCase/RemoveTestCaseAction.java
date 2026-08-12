package org.testin.testCase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GeneratorType;
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.Shortcuts;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RemoveTestCaseAction extends DumbAwareAction {
    private final @NotNull DirectoryDto dir;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull Project p;

    public RemoveTestCaseAction(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull JBList<TestCaseDto> list, final @NotNull CollectionListModel<TestCaseDto> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.p = p;
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(Shortcuts.DeletePackage.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        final boolean isCutAndSelected = TestEditorContextMenu.isGlobalCutAction() &&
                selectedItems.stream().allMatch(tc -> TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId()));

        if (!isCutAndSelected && !RemoveTestCaseDialog.confirmDeleteAction(p, selectedItems)) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedItems));
    }

    private void performDeletion(final @NotNull List<TestCaseDto> selectedItems) {
        relinkAroundRemoved(selectedItems);

        final var indexer = Services.getInstance(p, org.testin.indexer.ProjectIndexer.class);
        for (final TestCaseDto tc : selectedItems) {
            indexer.removeTestCase(dir.getPath(), tc.getId());
            GeneratorType.REMOVE_TEST_CASE.getAction().execute(p, tc);
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
     */
    private void relinkAroundRemoved(final @NotNull List<TestCaseDto> selectedItems) {
        final Set<UUID> removedIds = new HashSet<>();
        for (final TestCaseDto tc : selectedItems) removedIds.add(tc.getId());

        TestCaseDto prevSurvivor = null;
        int i = 0;
        while (i < model.getSize()) {
            final TestCaseDto tc = model.getElementAt(i);
            if (!removedIds.contains(tc.getId())) {
                prevSurvivor = tc;
                i++;
                continue;
            }

            // A run of removed rows: find the survivor after it.
            int j = i;
            while (j < model.getSize() && removedIds.contains(model.getElementAt(j).getId())) j++;
            final TestCaseDto nextSurvivor = j < model.getSize() ? model.getElementAt(j) : null;

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