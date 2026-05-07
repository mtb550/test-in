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
import org.testin.pojo.Config;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.ui.RemoveTestCaseDialog;
import org.testin.util.KeyboardSet;

import java.io.IOException;
import java.util.List;

public class RemoveTestCase extends DumbAwareAction {
    private final DirectoryDto dir;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;

    public RemoveTestCase(DirectoryDto dir, JBList<TestCaseDto> list, CollectionListModel<TestCaseDto> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.DeletePackage.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCaseDto> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (!RemoveTestCaseDialog.confirmDeleteAction(selectedItems)) return;

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                performDeletion(selectedItems);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
    }

    private void performDeletion(List<TestCaseDto> selectedItems) throws IOException {
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

        VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir.getPath().toFile());
        if (dirVFile == null) return;

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            TestCaseDto tc = selectedItems.get(i);

            VirtualFile targetFile = dirVFile.findChild(tc.getId() + ".json");
            if (targetFile != null) {
                targetFile.delete(this);
            }

            model.remove(model.getElementIndex(tc));
        }
    }

    private void saveToFile(TestCaseDto item) throws IOException {
        VirtualFile dirVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(dir.getPath().toFile());
        if (dirVFile == null) return;

        String fileName = item.getId() + ".json";
        VirtualFile targetFile = dirVFile.findChild(fileName);

        if (targetFile == null) {
            targetFile = dirVFile.createChildData(this, fileName);
        }

        String jsonContent = Config.getMapper().writerWithDefaultPrettyPrinter().writeValueAsString(item);
        VfsUtil.saveText(targetFile, jsonContent);
    }
}