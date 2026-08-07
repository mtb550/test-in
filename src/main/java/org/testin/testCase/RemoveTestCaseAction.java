package org.testin.testCase;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.testEditor.TestEditorCM;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.services.Services;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class RemoveTestCaseAction extends DumbAwareAction {
    private final DirectoryDto dir;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final Project project;

    public RemoveTestCaseAction(final @NotNull Project p, final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.project = p;
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.DeletePackage.getCustomShortcut(), list);
    }

    public static void deletePhysicalFiles(final List<TestCaseDto> items, final Path dirPath, final Object requestor) {
        VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dirPath.toFile());
        if (dirVFile == null) return;

        for (TestCaseDto tc : items) {
            VirtualFile targetFile = dirVFile.findChild(tc.getId() + ".json");
            if (targetFile != null) {
                try {
                    targetFile.delete(requestor);
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        boolean isCutAndSelected = TestEditorCM.isGlobalCutAction() &&
                selectedItems.stream().allMatch(tc -> TestEditorCM.getGlobalPendingCutIds().contains(tc.getId()));

        if (!isCutAndSelected && !RemoveTestCaseDialog.confirmDeleteAction(e.getProject(), selectedItems)) {
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

        final var indexer = Services.getInstance(project, org.testin.util.indexer.ProjectIndexer.class);
        for (final TestCaseDto tc : selectedItems) {
            indexer.removeTestCase(dir.getPath(), tc.getId());
        }

        deletePhysicalFiles(selectedItems, dir.getPath(), this);

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            model.remove(model.getElementIndex(selectedItems.get(i)));
        }
    }

    private void saveToFile(TestCaseDto item) {
        VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir.getPath().toFile());
        if (dirVFile == null) return;

        String fileName = item.getId() + ".json";
        VirtualFile targetFile = dirVFile.findChild(fileName);

        try {
            if (targetFile == null) {
                targetFile = dirVFile.createChildData(this, fileName);
            }

            String json = Services.getInstance(project, Mapper.class).writeValueAsString(item);
            VfsUtil.saveText(targetFile, json);

        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
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