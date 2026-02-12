package testGit.util;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.DirectoryType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class Directory {
    @Getter
    @Setter
    private static DefaultTreeModel testCasesTreeModel;
    @Getter
    @Setter
    private static DefaultTreeModel testPlansTreeModel;

    /**
     * بناء شجرة الحالات الاختبارية (Test Cases)
     */
    public static void buildTestCasesTree() {
        testCasesTreeModel = new DefaultTreeModel(buildRoot("TEST CASES", "testCases"));
    }

    /**
     * بناء شجرة الخطط الاختبارية (Test Plans)
     */
    public static void buildTestPlansTree() {
        testPlansTreeModel = new DefaultTreeModel(buildRoot("TEST PLANS", "testPlans"));
    }

    /**
     * دالة عامة لبناء الجذر الأساسي لتجنب تكرار الكود
     */
    private static DefaultMutableTreeNode buildRoot(String rootName, String subFolderName) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootName);
        File[] projects = Config.getRootFolderFile().listFiles(File::isDirectory);

        if (projects != null) {
            Arrays.stream(projects)
                    .filter(file -> !file.getName().startsWith(".")) // تجاهل .git والمجلدات المخفية
                    .map(Directory::map)
                    .filter(Objects::nonNull)
                    .forEach(dir -> rootNode.add(buildNodeRecursive(dir, subFolderName)));
        }
        return rootNode;
    }

    /**
     * دالة التكرار الذاتي (Recursion) الموحدة لكل أنواع الأشجار
     */
    public static DefaultMutableTreeNode buildNodeRecursive(@NotNull testGit.pojo.Directory dir, @Nullable String subFolder) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(dir);

        // إذا كان هناك مجلد وسيط (مثل testCases/testPlans) نبحث بداخله، وإلا نبحث في مجلد dir المباشر
        File folderToScan = (subFolder != null && dir.getFilePath() != null)
                ? dir.getFilePath().resolve(subFolder).toFile()
                : dir.getFile();

        File[] children = folderToScan.listFiles(File::isDirectory);

        if (children != null) {
            for (File childFile : children) {
                testGit.pojo.Directory childDir = map(childFile);
                if (childDir != null) {
                    // نمرر null في المستوى التالي لأن الهيكل داخلياً متداخل مباشرة
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
    public static testGit.pojo.Directory map(@NotNull final File file) {
        try {
            String[] parts = file.getName().split("_", 3);

            return new testGit.pojo.Directory()
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