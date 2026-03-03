package testGit.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
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

public class TestRunsDirectoryMapper {

    public static void buildTreeAsync(@NotNull SimpleTree tree) {
        Project project = DataManager.getInstance().getDataContext(tree).getData(CommonDataKeys.PROJECT);
        if (project == null) project = Config.getProject();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading test runs", false) {
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
                if (newModel != null) {
                    tree.setModel(newModel);
                    newModel.nodeStructureChanged((DefaultMutableTreeNode) newModel.getRoot());
                    tree.updateUI();
                    tree.expandRow(0);
                }
            }
        });
    }

    private static DefaultMutableTreeNode buildRoot(String rootName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] projects = Config.getRootFolderFile().listFiles();

        if (projects != null) {
            Arrays.stream(projects)
                    .filter(file -> !file.getName().startsWith("."))
                    .map(TestRunsDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(dir -> {
                        System.out.println("TestRunsDirectoryMapper.buildRoot: " + dir.getName());
                        rootNode.add(buildNodeRecursive(dir, "testRuns"));
                    });
        }
        return rootNode;
    }

    public static DefaultMutableTreeNode buildNodeRecursive(@NotNull Directory dir, @Nullable String subFolder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        File folderToScan = (subFolder != null && dir.getFilePath() != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        System.out.println("TestRunsDirectoryMapper.buildNodeRecursive. folderToScan: " + folderToScan);

        File[] runFiles = folderToScan.listFiles();

        if (runFiles != null) {
            Arrays.stream(runFiles)
                    .map(TestRunsDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(runDir -> {
                        System.out.println("TestRunsDirectoryMapper.buildNodeRecursive: " + runDir.getName());
                        node.add(buildNodeRecursive(runDir, null));
                    });
        }
        return node;
    }

    @Nullable
    public static Directory map(@NotNull final File file) {
        try {
            String fileName = file.getName();
            System.out.println("TestRunsDirectoryMapper.map(). fileName: " + fileName);

            String rawName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;

            String[] parts = rawName.split("_", 3);
            if (parts.length < 3) return null;

            return new Directory()
                    .setFile(file)
                    .setFilePath(file.toPath())
                    .setFileName(file.getName())
                    .setType(DirectoryType.valueOf(parts[0].toUpperCase()))
                    .setName(parts[1])
                    .setActive(Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }
}