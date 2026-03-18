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
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.dirs.DirectoryDto;
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
    private final JBList<TestCaseDto> list;
    private final DirectoryDto dir;
    private final CollectionListModel<TestCaseDto> model;

    public CreateTestCase(DirectoryDto dir, JBList<TestCaseDto> list, CollectionListModel<TestCaseDto> model) {
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
        TestCaseDto newTestCaseDto = new TestCaseDto();
        newTestCaseDto.setId(UUID.randomUUID().toString());
        newTestCaseDto.setTitle(dialog.getTitle());
        newTestCaseDto.setPriority(Priority.valueOf(dialog.getPriority()));
        newTestCaseDto.setGroups(dialog.getSelectedGroups());
        newTestCaseDto.setNext(null);

        try {
            if (model.isEmpty()) {
                newTestCaseDto.setIsHead(true);
            } else {
                newTestCaseDto.setIsHead(false);
                TestCaseDto lastItem = model.getElementAt(model.getSize() - 1);
                lastItem.setNext(UUID.fromString(newTestCaseDto.getId()));

                File lastItemFile = new File(dir.getPath().toFile(), lastItem.getId() + ".json");
                Config.getMapper().writeValue(lastItemFile, lastItem);
            }

            File targetFile = new File(dir.getPath().toFile(), newTestCaseDto.getId() + ".json");
            Config.getMapper().writeValue(targetFile, newTestCaseDto);

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);
            model.add(newTestCaseDto);
            list.ensureIndexIsVisible(model.getSize() - 1);

        } catch (IOException ex) {
            Notifier.error("Error", "unable to add new test case. " + ex.getMessage());
        }
    }

    private List<String> getTreePathNames(DefaultMutableTreeNode node) {
        List<String> pathNames = new ArrayList<>();
        TreeNode[] nodes = node.getPath();
        for (TreeNode n : nodes) {
            if (n instanceof DefaultMutableTreeNode dmtn && dmtn.getUserObject() instanceof DirectoryDto dir) {
                pathNames.add(dir.getName());
            }
        }
        return pathNames;
    }

}