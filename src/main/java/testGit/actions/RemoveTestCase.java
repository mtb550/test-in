package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.ui.RemoveTestCaseDialog;
import testGit.util.KeyboardSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

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

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            TestCaseDto tc = selectedItems.get(i);
            File file = new File(dir.getPath().toFile(), tc.getId() + ".json");
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            model.remove(model.getElementIndex(tc));
        }

        LocalFileSystem.getInstance().refreshIoFiles(List.of(Objects.requireNonNull(dir.getPath().toFile().listFiles())));
    }

    private void saveToFile(TestCaseDto item) throws IOException {
        File file = new File(dir.getPath().toFile(), item.getId() + ".json");
        Config.getMapper().writeValue(file, item);
    }
}