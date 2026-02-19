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

public class TestCasesDirectoryMapper {
    @Getter
    @Setter
    private static DefaultTreeModel treeModel;

    /**
     * بناء شجرة الحالات الاختبارية (Test Cases)
     */
    public static void buildTree() {
        treeModel = new DefaultTreeModel(buildRoot(ProjectSelector.getSelectedProject().getName(), "testCases"));
    }

    /**
     * دالة عامة لبناء الجذر الأساسي لتجنب تكرار الكود
     */
    private static DefaultMutableTreeNode buildRoot(String rootName, String subFolderName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] projects = Config.getRootFolderFile().listFiles(File::isDirectory);

        if (projects != null) {
            Arrays.stream(projects)
                    .filter(file -> !file.getName().startsWith("."))
                    .map(TestCasesDirectoryMapper::map)
                    .filter(Objects::nonNull)
                    .forEach(dir -> {
                        System.out.println("TestCasesDirectoryMapper.buildRoot(). " + dir.getName());
                        rootNode.add(buildNodeRecursive(dir, subFolderName));
                    });
        }
        return rootNode;
    }

    /**
     * دالة التكرار الذاتي (Recursion) الموحدة لكل أنواع الأشجار
     */
    public static DefaultMutableTreeNode buildNodeRecursive(@NotNull Directory dir, @Nullable String subFolder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        File folderToScan = (subFolder != null && dir.getFilePath() != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        File[] children = folderToScan.listFiles(File::isDirectory);

        if (children != null) {
            for (File childFile : children) {
                Directory childDir = map(childFile);
                if (childDir != null) {
                    node.add(buildNodeRecursive(childDir, null));
                }
            }
        }
        return node;
    }

    /**
     * تحويل File إلى كائن Directory مع معالجة الأخطاء
     */
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