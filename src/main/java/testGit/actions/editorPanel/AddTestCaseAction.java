package testGit.actions.editorPanel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.TestCase;
import testGit.ui.AddNewTestCaseDialog;
import testGit.util.Notifier;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AddTestCaseAction extends AnAction {
    private final JBList<TestCase> list;
    private final String featurePath;
    private final CollectionListModel<TestCase> model;

    public AddTestCaseAction(String featurePath, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("➕ Add Test Case");
        this.list = list;
        this.featurePath = featurePath;
        this.model = model;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AddNewTestCaseDialog dialog = new AddNewTestCaseDialog();

        if (dialog.showAndGet()) {
            saveNewTestCase(dialog);
        }
    }

    private void saveNewTestCase(AddNewTestCaseDialog dialog) {
        TestCase newCase = new TestCase();
        newCase.setId(UUID.randomUUID().toString());
        newCase.setTitle(dialog.getTitle());
        newCase.setPriority(dialog.getPriority());
        newCase.setGroups(dialog.getSelectedGroups());
        newCase.setNext(null);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Linked list chain logic
            if (model.isEmpty()) {
                newCase.setIsHead(true);
            } else {
                newCase.setIsHead(false);
                TestCase lastItem = model.getElementAt(model.getSize() - 1);
                lastItem.setNext(UUID.fromString(newCase.getId()));

                File lastItemFile = new File(featurePath, lastItem.getId() + ".json");
                mapper.writeValue(lastItemFile, lastItem);
            }

            File targetFile = new File(featurePath, newCase.getId() + ".json");
            mapper.writeValue(targetFile, newCase);

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            model.add(newCase);
            list.ensureIndexIsVisible(model.getSize() - 1);

        } catch (IOException ex) {
            Notifier.notify(Config.getProject(), "Test Case Notifications", "Error", ex.getMessage(), NotificationType.ERROR);
        }
    }
}