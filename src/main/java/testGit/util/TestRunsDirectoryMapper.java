package testGit.util;

import lombok.Getter;
import lombok.Setter;
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
    @Getter
    @Setter
    private static DefaultTreeModel treeModel;

    /**
     * Builds the Test Runs tree.
     * Matches the 'buildTree' naming in TestCasesDirectoryMapper.
     */
    public static void buildTree() {
        String projectName = ProjectSelector.getSelectedProject().getName();
        treeModel = new DefaultTreeModel(buildRoot(projectName, "testRuns"));
    }

    private static DefaultMutableTreeNode buildRoot(String rootName, String subFolderName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] projects = Config.getRootFolderFile().listFiles();

        if (projects != null) {
            Arrays.stream(projects)
                    .filter(file -> !file.getName().startsWith("."))
                    .map(TestRunsDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(dir -> {
                        System.out.println("TestRunsDirectoryMapper.buildRoot: " + dir.getName());
                        rootNode.add(buildNodeRecursive(dir, subFolderName));
                    });
        }
        return rootNode;
    }

    /**
     * Recursive-style node builder to match TestCasesDirectoryMapper structure.
     */
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

    /**
     * Maps a File to a Directory object.
     * Renamed from 'mapToRun' to 'map' to match TestCasesDirectoryMapper.
     */
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