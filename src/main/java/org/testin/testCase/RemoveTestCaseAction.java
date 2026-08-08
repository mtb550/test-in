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
import org.testin.editorPanel.testEditor.TestEditorContextMenu;
import org.testin.generateJavaCode.GeneratorType;
import org.testin.indexer.ProjectIndexer;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.KeyboardSet;

import java.util.List;

public class RemoveTestCaseAction extends DumbAwareAction {
    private final DirectoryDto dir;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final @NotNull Project p;

    public RemoveTestCaseAction(final @NotNull Project p, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.p = p;
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.DeletePackage.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        boolean isCutAndSelected = TestEditorContextMenu.isGlobalCutAction() &&
                selectedItems.stream().allMatch(tc -> TestEditorContextMenu.getGlobalPendingCutIds().contains(tc.getId()));

        if (!isCutAndSelected && !RemoveTestCaseDialog.confirmDeleteAction(p, selectedItems)) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> performDeletion(selectedItems));
    }

    private void performDeletion(final List<TestCaseDto> selectedItems) {
        int firstIdx = model.getElementIndex(selectedItems.getFirst());
        int lastIdx = model.getElementIndex(selectedItems.getLast());

        TestCaseDto successor = (model.getSize() > lastIdx + 1) ? model.getElementAt(lastIdx + 1) : null;

        if (firstIdx == 0) {
            if (successor != null) {
                successor.setIsHead(true);
                saveToFile(successor);
            }
        } else {
            TestCaseDto predecessor = model.getElementAt(firstIdx - 1);
            predecessor.setNext(successor != null ? successor.getId() : null);
            saveToFile(predecessor);
        }

        final var indexer = Services.getInstance(p, org.testin.indexer.ProjectIndexer.class);
        for (final TestCaseDto tc : selectedItems) {
            indexer.removeTestCase(dir.getPath(), tc.getId());
            GeneratorType.REMOVE_TEST_CASE.getAction().execute(p, tc);
        }

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            model.remove(model.getElementIndex(selectedItems.get(i)));
        }
    }

    private void saveToFile(TestCaseDto item) {
        Services.getInstance(p, ProjectIndexer.class).putTestCase(dir.getPath(), item);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!list.isEmpty() && !list.getSelectedValuesList().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}