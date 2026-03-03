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

public class TestCasesDirectoryMapper {

    public static void buildTreeAsync(@NotNull SimpleTree tree) {

        ProgressManager.getInstance().run(new Task.Backgroundable(Config.getProject(), "Loading test cases", false) {
            private DefaultTreeModel newModel;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Directory selectedProject = ProjectSelector.getSelectedProject();

                if (selectedProject == null) {
                    return;
                }

                indicator.setIndeterminate(true);
                indicator.setText("Scanning directories for test cases...");

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
                System.out.println("TC Tree loaded successfully in background.");
            }
        });
    }

    private static DefaultMutableTreeNode buildRoot(String rootName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] testProjects = Config.getTestGitPath().toFile().listFiles(File::isDirectory);

        if (testProjects != null) {
            Arrays.stream(testProjects)
                    .filter(file -> !file.getName().startsWith("."))
                    .map(TestCasesDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(item -> {
                        System.out.println("TestCasesDirectoryMapper.buildRoot(). " + item.getFileName());
                        rootNode.add(buildNodeRecursive(item, "testCases"));
                    });
        }
        return rootNode;
    }

    public static DefaultMutableTreeNode buildNodeRecursive(@NotNull Directory dir, @Nullable String subFolder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        File folderToScan = (subFolder != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        Optional.ofNullable(folderToScan.listFiles(File::isDirectory)) // Handles the null check safely
                .stream()
                .flatMap(Arrays::stream)
                .map(TestCasesDirectoryMapper::map)
                .parallel()
                .filter(Objects::nonNull)
                .forEachOrdered(caseDir -> {
                    System.out.println("TestCasesDirectoryMapper.buildNodeRecursive: " + caseDir.getName());
                    node.add(buildNodeRecursive(caseDir, null));
                });

        return node;
    }

    @Nullable
    public static Directory map(@NotNull final File file) {
        try {
            String[] parts = file.getName().split("_", 3);

            return new Directory()
                    .setFile(file)
                    .setFilePath(file.toPath())
                    .setFileName(file.getName())
                    .setType(DirectoryType.valueOf(parts[0].toUpperCase()))
                    .setName(parts[1])
                    .setActive(Integer.parseInt(parts[2]));
        } catch (Exception e) {
            System.err.println("Skipping invalid directory format: " + file.getName());
            return null;
        }
    }
}