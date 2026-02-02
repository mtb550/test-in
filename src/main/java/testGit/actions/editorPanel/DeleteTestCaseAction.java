package testGit.actions.editorPanel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.TestCase;
import testGit.ui.DeleteTestCaseDialog;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class DeleteTestCaseAction extends AnAction {
    private final String featurePath;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final ObjectMapper mapper;

    public DeleteTestCaseAction(String featurePath, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("🗑 Delete");
        this.featurePath = featurePath;
        this.list = list;
        this.model = model;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        List<TestCase> selectedItems = list.getSelectedValuesList();
        if (selectedItems.isEmpty()) return;

        if (!DeleteTestCaseDialog.confirmDeleteAction(selectedItems)) return;

        // Perform file operations in a background task or write action
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                performDeletion(selectedItems);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        });
    }

    private void performDeletion(List<TestCase> selectedItems) throws IOException {
        // Find indices to identify context
        int firstIdx = model.getElementIndex(selectedItems.get(0));
        int lastIdx = model.getElementIndex(selectedItems.get(selectedItems.size() - 1));

        // Get successor (the first item AFTER the entire selection)
        TestCase successor = (model.getSize() > lastIdx + 1) ? model.getElementAt(lastIdx + 1) : null;

        // 1. Repair Chain
        if (firstIdx == 0) {
            // Deleting the head: next remaining item becomes head
            if (successor != null) {
                successor.setIsHead(true);
                saveToFile(successor);
            }
        } else {
            // Not deleting head: previous item points to successor
            TestCase predecessor = model.getElementAt(firstIdx - 1);
            predecessor.setNext(successor != null ? UUID.fromString(successor.getId()) : null);
            saveToFile(predecessor);
        }

        // 2. Delete files and update UI model
        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            TestCase tc = selectedItems.get(i);

            // Delete from Disk
            File file = new File(featurePath, tc.getId() + ".json");
            if (file.exists()) {
                Files.delete(file.toPath());
            }

            // Remove from UI
            model.remove(model.getElementIndex(tc));
        }

        // 3. Batch Refresh VFS (Performance efficient)
        LocalFileSystem.getInstance().refreshIoFiles(List.of(new File(featurePath).listFiles()));
    }

    private void saveToFile(TestCase item) throws IOException {
        File file = new File(featurePath, item.getId() + ".json");
        mapper.writeValue(file, item);
    }
}