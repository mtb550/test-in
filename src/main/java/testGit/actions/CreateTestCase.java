package testGit.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.pojo.TestCase;
import testGit.ui.CreateNewTestCaseDialog;
import testGit.util.Notifier;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CreateTestCase extends AnAction {
    private final JBList<TestCase> list;
    private final Directory dir;
    private final CollectionListModel<TestCase> model;

    public CreateTestCase(Directory dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.list = list;
        this.dir = dir;
        this.model = model;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("CreateTestCaseAction.actionPerformed(). dirPath: " + dir.getFilePath());
        CreateNewTestCaseDialog dialog = new CreateNewTestCaseDialog();

        if (dialog.showAndGet()) {
            saveNewTestCase(dialog);
        }
    }

    private void saveNewTestCase(CreateNewTestCaseDialog dialog) {
        TestCase newTestCase = new TestCase();
        newTestCase.setId(UUID.randomUUID().toString());
        newTestCase.setTitle(dialog.getTitle());
        newTestCase.setPriority(dialog.getPriority());
        newTestCase.setGroups(dialog.getSelectedGroups());
        newTestCase.setNext(null);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        try {
            if (model.isEmpty()) {
                newTestCase.setIsHead(true);
            } else {
                newTestCase.setIsHead(false);
                TestCase lastItem = model.getElementAt(model.getSize() - 1);
                lastItem.setNext(UUID.fromString(newTestCase.getId()));

                File lastItemFile = new File(dir.getFile(), lastItem.getId() + ".json");
                mapper.writeValue(lastItemFile, lastItem);
            }

            File targetFile = new File(dir.getFile(), newTestCase.getId() + ".json");
            mapper.writeValue(targetFile, newTestCase);

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            model.add(newTestCase);
            list.ensureIndexIsVisible(model.getSize() - 1);

        } catch (IOException ex) {
            Notifier.error("Error", "unable to add new test case. " + ex.getMessage());
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}