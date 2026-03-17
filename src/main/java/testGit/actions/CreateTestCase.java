package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.Priority;
import testGit.pojo.mappers.TestCaseJsonMapper;
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
    private final JBList<TestCaseJsonMapper> list;
    private final Directory dir;
    private final CollectionListModel<TestCaseJsonMapper> model;

    public CreateTestCase(Directory dir, JBList<TestCaseJsonMapper> list, CollectionListModel<TestCaseJsonMapper> model) {
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
        TestCaseJsonMapper newTestCaseJsonMapper = new TestCaseJsonMapper();
        newTestCaseJsonMapper.setId(UUID.randomUUID().toString());
        newTestCaseJsonMapper.setTitle(dialog.getTitle());
        newTestCaseJsonMapper.setPriority(Priority.valueOf(dialog.getPriority()));
        newTestCaseJsonMapper.setGroups(dialog.getSelectedGroups());
        newTestCaseJsonMapper.setNext(null);

        try {
            if (model.isEmpty()) {
                newTestCaseJsonMapper.setIsHead(true);
            } else {
                newTestCaseJsonMapper.setIsHead(false);
                TestCaseJsonMapper lastItem = model.getElementAt(model.getSize() - 1);
                lastItem.setNext(UUID.fromString(newTestCaseJsonMapper.getId()));

                File lastItemFile = new File(dir.getPath().toFile(), lastItem.getId() + ".json");
                Config.getMapper().writeValue(lastItemFile, lastItem);
            }

            File targetFile = new File(dir.getPath().toFile(), newTestCaseJsonMapper.getId() + ".json");
            Config.getMapper().writeValue(targetFile, newTestCaseJsonMapper);

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            model.add(newTestCaseJsonMapper);
            list.ensureIndexIsVisible(model.getSize() - 1);

        } catch (IOException ex) {
            Notifier.error("Error", "unable to add new test case. " + ex.getMessage());
        }
    }

    private List<String> getTreePathNames(DefaultMutableTreeNode node) {
        List<String> pathNames = new ArrayList<>();
        TreeNode[] nodes = node.getPath();
        for (TreeNode n : nodes) {
            if (n instanceof DefaultMutableTreeNode dmtn && dmtn.getUserObject() instanceof Directory dir) {
                pathNames.add(dir.getName());
            }
        }
        return pathNames;
    }

}