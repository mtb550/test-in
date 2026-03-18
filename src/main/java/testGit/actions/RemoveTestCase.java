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
import testGit.pojo.mappers.TestCase;
import testGit.pojo.tree.dirs.Directory;
import testGit.ui.RemoveTestCaseDialog;
import testGit.util.KeyboardSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RemoveTestCase extends DumbAwareAction {
    private final Directory dir;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;

    public RemoveTestCase(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("Delete", "Delete test case", AllIcons.Actions.DeleteTag);
        this.dir = dir;
        this.list = list;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.DeletePackage.getShortcut(), list);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCase> selectedItems = list.getSelectedValuesList();
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

    private void performDeletion(List<TestCase> selectedItems) throws IOException {
        int firstIdx = model.getElementIndex(selectedItems.get(0));
        int lastIdx = model.getElementIndex(selectedItems.get(selectedItems.size() - 1));

        TestCase successor = (model.getSize() > lastIdx + 1) ? model.getElementAt(lastIdx + 1) : null;

        if (firstIdx == 0) {
            if (successor != null) {
                successor.setIsHead(true);
                saveToFile(successor);
            }
        } else {
            TestCase predecessor = model.getElementAt(firstIdx - 1);
            predecessor.setNext(successor != null ? UUID.fromString(successor.getId()) : null);
            saveToFile(predecessor);
        }

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            TestCase tc = selectedItems.get(i);
            File file = new File(dir.getPath().toFile(), tc.getId() + ".json");
            if (file.exists()) {
                Files.delete(file.toPath());
            }
            model.remove(model.getElementIndex(tc));
        }

        LocalFileSystem.getInstance().refreshIoFiles(List.of(Objects.requireNonNull(dir.getPath().toFile().listFiles())));
    }

    private void saveToFile(TestCase item) throws IOException {
        File file = new File(dir.getPath().toFile(), item.getId() + ".json");
        Config.getMapper().writeValue(file, item);
    }
}