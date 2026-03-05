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
    DefaultMutableTreeNode rootNode;

    public TestCaseTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = new SimpleTree();
    }


    public void init() {
        System.out.println("TestCaseTabController.init()");

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();
        tree.setCellRenderer(new TestCaseRenderer(sharedCutNodes));

        TransferHandlerImpl transferHandler = new TransferHandlerImpl(tree, sharedCutNodes);
        tree.setTransferHandler(transferHandler);
        ShortcutHandler.register(projectPanel, tree, transferHandler);
        tree.addMouseListener(new MouseAdapterImpl(projectPanel));

        // ENHANCEMENT: Configure the empty state message
        tree.getEmptyText().clear();
        tree.getEmptyText().appendLine("No test cases found.");
        tree.getEmptyText().appendLine("Add new package", SimpleTextAttributes.LINK_ATTRIBUTES, e -> {
            // Trigger the Create Package action manually
            new CreateTestCasePackage(projectPanel, tree).actionPerformed(null);
        });
        tree.getEmptyText().appendLine("Add new test set", SimpleTextAttributes.LINK_ATTRIBUTES, e -> {
            // Trigger the Create Test Set action manually
            new CreateTestSet(tree);
        });

        System.out.println("once init tc: " + projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());

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
                if (rootNode.getChildCount() == 0) {
                    System.out.println("No packages found under TEST CASES.");
                    tree.setRootVisible(false);
                } else
                    tree.setRootVisible(true);

                tree.setShowsRootHandles(false);
                tree.setDragEnabled(true);
                tree.setDropMode(DropMode.ON_OR_INSERT);
                tree.setEnabled(true);
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
                .parallel()
                .map(DirectoryMapper::map)
                .filter(Objects::nonNull)
                .forEachOrdered(caseDir -> node.add(buildNodeRecursive(caseDir)));

        return node;
    }

}