package testGit.projectPanel.testRunTab;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.util.DirectoryMapper;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class TestRunTabController {
    private final ProjectPanel projectPanel;
    @Getter
    private final SimpleTree tree;

    public TestRunTabController(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.tree = new SimpleTree();
    }

    public void init() {
        System.out.println("TestRunTabController.init()");

        tree.setCellRenderer(new TestRunRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(false);
        tree.addMouseListener(new MouseAdapterImpl(projectPanel));
        ShortcutHandler.register(projectPanel, tree);

        buildTreeAsync(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
    }

    public void buildTreeAsync(Directory selectedProject) {
        System.out.println("TestRunTabController.buildTreeAsync()");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DefaultMutableTreeNode root = buildNodeRecursive(selectedProject, "testRuns");
            DefaultTreeModel newModel = new DefaultTreeModel(root);

            ApplicationManager.getApplication().invokeLater(() -> {
                this.tree.setModel(newModel);
                this.tree.setRootVisible(true);
                this.tree.revalidate();
                this.tree.repaint();
            });
        });
    }

    private DefaultMutableTreeNode buildNodeRecursive(Directory dir, String subFolder) {
        System.out.println("TR buildNodeRecursive");
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        File folderToScan = (subFolder != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        Optional.ofNullable(folderToScan.listFiles())
                .stream()
                .flatMap(Arrays::stream)
                //.parallel()
                .map(DirectoryMapper::map)
                .filter(Objects::nonNull)
                .forEachOrdered(runDir -> node.add(buildNodeRecursive(runDir, null)));

        return node;
    }
}