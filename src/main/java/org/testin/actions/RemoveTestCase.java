package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.EditorCM;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.RemoveTestCaseDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.Mapper;
import org.testin.util.logger.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class RemoveTestCase extends DumbAwareAction {
    private final DirectoryDto dir;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;

    public RemoveTestCase(final DirectoryDto dir, final JBList<TestCaseDto> list, final CollectionListModel<TestCaseDto> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
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
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        boolean isCutAndSelected = EditorCM.isGlobalCutAction() &&
                selectedItems.stream().allMatch(tc -> EditorCM.getGlobalPendingCutIds().contains(tc.getId()));

        if (!isCutAndSelected && !RemoveTestCaseDialog.confirmDeleteAction(selectedItems)) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                performDeletion(selectedItems);
            } catch (IOException ex) {
                Log.error("Exception: " + ex.getMessage());
            }
        });
    }

    private void performDeletion(final List<TestCaseDto> selectedItems) throws IOException {
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

            String jsonContent = Mapper.writeValueAsString(item);
            VfsUtil.saveText(targetFile, jsonContent);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}