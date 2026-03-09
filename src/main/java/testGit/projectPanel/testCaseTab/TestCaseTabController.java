package testGit.projectPanel.testCaseTab;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import testGit.actions.CreateTestCasePackage;
import testGit.actions.CreateTestSet;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.TransferHandlerImpl;
import testGit.util.DirectoryMapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.*;

public class TestCaseTabController {
    @Getter
    public final SimpleTree tree;
    private final ProjectPanel projectPanel;
    @Getter
    private DefaultMutableTreeNode rootNode;

    public TestCaseTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = new SimpleTree();
        tree.setRootVisible(false);
    }

    public void init() {
        System.out.println("TestCaseTabController.init()");

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();

        tree.setCellRenderer(new TestCaseRenderer(sharedCutNodes));
        TransferHandlerImpl transferHandler = new TransferHandlerImpl(tree, sharedCutNodes);
        tree.setTransferHandler(transferHandler);
        ShortcutHandler.register(projectPanel, tree, transferHandler);
        tree.addMouseListener(new MouseAdapterImpl(projectPanel));

        showEmptyState();
        System.out.println("once init tc: " + projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
    }

    private void showEmptyState() {
        tree.getEmptyText().clear();

        tree.getEmptyText().appendLine("Create new package", SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new CreateTestCasePackage(projectPanel, tree).actionPerformed(null));

        tree.getEmptyText().appendLine("Create new test set", SimpleTextAttributes.LINK_ATTRIBUTES,
                e -> new CreateTestSet(null).actionPerformed(null));
    }

    public void buildTreeAsync(Directory selectedProject) {
        System.out.println("TestCaseTabController.buildTreeAsync()");

        rootNode = new DefaultMutableTreeNode("TEST CASES");
        File testCasesFolder = selectedProject.getFilePath().resolve("testCases").toFile();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (testCasesFolder.exists() && testCasesFolder.isDirectory()) {
                Optional.ofNullable(testCasesFolder.listFiles(File::isDirectory))
                        .stream()
                        .flatMap(Arrays::stream)
                        .map(DirectoryMapper::map)
                        .filter(Objects::nonNull)
                        .forEachOrdered(caseDir -> rootNode.add(buildNodeRecursive(caseDir)));
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultTreeModel newModel = new DefaultTreeModel(rootNode);

                tree.setRootVisible(rootNode.getChildCount() > 0);
                tree.setShowsRootHandles(true);
                tree.setDragEnabled(true);
                tree.setDropMode(DropMode.ON_OR_INSERT);
                tree.setModel(newModel);
                TreeUtil.expandAll(tree);
                tree.revalidate();
                tree.repaint();
            });
        });
    }

    private DefaultMutableTreeNode buildNodeRecursive(@NotNull Directory dir) {
        System.out.println("TC buildNodeRecursive");

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        Optional.ofNullable(dir.getFile().listFiles(File::isDirectory))
                .stream()
                .flatMap(Arrays::stream)
                //.parallel()
                .map(DirectoryMapper::map)
                .filter(Objects::nonNull)
                .forEachOrdered(caseDir -> node.add(buildNodeRecursive(caseDir)));

        return node;
    }

}