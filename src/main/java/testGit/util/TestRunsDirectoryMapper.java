package testGit.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
import testGit.projectPanel.projectSelector.ProjectSelector;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class TestRunsDirectoryMapper {

    public static void buildTreeAsync(SimpleTree tree) {

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Loading test runs", false) {
            private DefaultTreeModel newModel;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Directory selectedProject = ProjectSelector.getSelectedProject();

                if (selectedProject == null) {
                    return;
                }

                indicator.setIndeterminate(true);
                indicator.setText("Scanning directories for test runs...");

                String rootName = selectedProject.getName();
                DefaultMutableTreeNode root = buildRoot(rootName);
                newModel = new DefaultTreeModel(root);
            }

            @Override
            public void onSuccess() {
                tree.setModel(newModel);
                newModel.nodeStructureChanged((DefaultMutableTreeNode) newModel.getRoot());
                //tree.updateUI();
                tree.expandRow(0);
                System.out.println("TR Tree loaded successfully in background.");
            }
        });
    }

    private static DefaultMutableTreeNode buildRoot(String rootName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] testProjects = Config.getTestGitPath().toFile().listFiles();

        if (testProjects != null) {
            Arrays.stream(testProjects)
                    .filter(item -> !item.getName().startsWith("."))
                    .map(TestRunsDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(item -> {
                        System.out.println("TestRunsDirectoryMapper.buildRoot: " + item.getName());
                        rootNode.add(buildNodeRecursive(item, "testRuns"));
                    });
        }
        return rootNode;
    }

    public static DefaultMutableTreeNode buildNodeRecursive(Directory dir, String subFolder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        File folderToScan = (subFolder != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        System.out.println("TestRunsDirectoryMapper.buildNodeRecursive. folderToScan: " + folderToScan);

        Optional.ofNullable(folderToScan.listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .map(TestRunsDirectoryMapper::map)
                .parallel()
                .filter(Objects::nonNull)
                .forEachOrdered(runDir -> {
                    System.out.println("TestRunsDirectoryMapper.buildNodeRecursive: " + runDir.getFileName());
                    node.add(buildNodeRecursive(runDir, null));
                });

        return node;
    }

    @Nullable
    public static Directory map(final File file) {
        try {
            String fileName = file.getName();
            System.out.println("TestRunsDirectoryMapper.map(). fileName: " + fileName);

            String rawName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
            System.out.println("TestRunsDirectoryMapper.map(). rawName: " + rawName);

            String[] parts = rawName.split("_", 3);
            if (parts.length < 3)
                Notifier.error("Test run Error", "invalid name: " + rawName);

            return new Directory()
                    .setFile(file)
                    .setFilePath(file.toPath())
                    .setFileName(file.getName())
                    .setType(DirectoryType.valueOf(parts[0].toUpperCase()))
                    .setName(parts[1])
                    .setActive(Integer.parseInt(parts[2]));
        } catch (Exception e) {
            Notifier.error("mapping Failed", e.getMessage());
            return null;
        }
    }
}