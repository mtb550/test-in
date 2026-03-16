package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Priority;
import testGit.pojo.TestCase;
import testGit.pojo.TestPackage;
import testGit.ui.CreateTestCaseDialog;
import testGit.util.KeyboardSet;
import testGit.util.Notifier;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateTestCase extends DumbAwareAction {
    private final JBList<TestCase> list;
    private final TestPackage dir;
    private final CollectionListModel<TestCase> model;

    public CreateTestCase(TestPackage dir, JBList<TestCase> list, CollectionListModel<TestCase> model) {
        super("Create Test Case", "Create new test case", AllIcons.Actions.AddToDictionary);
        this.list = list;
        this.dir = dir;
        this.model = model;
        this.registerCustomShortcutSet(KeyboardSet.CreateTestCase.getShortcut(), list);

    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        CreateTestCaseDialog dialog = new CreateTestCaseDialog();
        if (dialog.showAndGet()) {
            saveNewTestCase(dialog);
        }
    }

    private void saveNewTestCase(CreateTestCaseDialog dialog) {
        TestCase newTestCase = new TestCase();
        newTestCase.setId(UUID.randomUUID().toString());
        newTestCase.setTitle(dialog.getTitle());
        newTestCase.setPriority(Priority.valueOf(dialog.getPriority()));
        newTestCase.setGroups(dialog.getSelectedGroups());
        newTestCase.setNext(null);

        try {
            if (model.isEmpty()) {
                newTestCase.setIsHead(true);
            } else {
                newTestCase.setIsHead(false);
                TestCase lastItem = model.getElementAt(model.getSize() - 1);
                lastItem.setNext(UUID.fromString(newTestCase.getId()));

                File lastItemFile = new File(dir.getFile(), lastItem.getId() + ".json");
                Config.getMapper().writeValue(lastItemFile, lastItem);
            }

            File targetFile = new File(dir.getFile(), newTestCase.getId() + ".json");
            Config.getMapper().writeValue(targetFile, newTestCase);

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            model.add(newTestCase);
            list.ensureIndexIsVisible(model.getSize() - 1);

        } catch (IOException ex) {
            Notifier.error("Error", "unable to add new test case. " + ex.getMessage());
        }
    }

    private List<String> getTreePathNames(DefaultMutableTreeNode node) {
        List<String> pathNames = new ArrayList<>();
        TreeNode[] nodes = node.getPath();
        for (TreeNode n : nodes) {
            if (n instanceof DefaultMutableTreeNode dmtn && dmtn.getUserObject() instanceof TestPackage dir) {
                pathNames.add(dir.getName());
            }
        }
        return pathNames;
    }

}