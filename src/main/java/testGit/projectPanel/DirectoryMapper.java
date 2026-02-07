package testGit.projectPanel;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.Config;
import testGit.pojo.Directory;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public class DirectoryMapper {
    @Getter
    public static DefaultTreeModel testCasesTreeModel;
    @Getter
    public static DefaultTreeModel testPlansTreeModel;

    public static void buildTestCasesTree(Directory selectedProject) {
        System.out.println("ExplorerTree.buildTestCasesTree()");
        System.out.println("selectedProject" + selectedProject);

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("TEST CASES");
        File[] children = Config.getTestCasesRootFolder(selectedProject).listFiles(File::isDirectory);

        if (children != null) {
            Arrays.stream(children).forEach(file -> System.out.println(file.getName()));

            Arrays.stream(children)
                    .map(DirectoryMapper::mapSuite)
                    .forEach(project -> rootNode.add(buildTestCasesSubTree(project)));
        }
        testCasesTreeModel = new DefaultTreeModel(rootNode);
    }

    public static void buildTestPlansTree(Directory selectedProject) {
        System.out.println("ExplorerTree.buildTestPlansTree()");
        System.out.println("selectedProject: " + selectedProject.getName());

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("TEST PLANS");
        File[] children = Config.getTestPlansRootFolder(selectedProject).listFiles(File::isDirectory);

        if (children != null) {
            Arrays.stream(children).forEach(file -> System.out.println(file.getName()));

            Arrays.stream(children)
                    .map(DirectoryMapper::mapSuite)
                    .forEach(project -> rootNode.add(buildTestPlansSubTree(project)));
        }

        testPlansTreeModel = new DefaultTreeModel(rootNode);
    }

    public static DefaultMutableTreeNode buildTestCasesSubTree(final Directory project) {
        System.out.println("ExplorerTree.buildTestCasesSubTree()");

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(project);
        Path p = project.getFilePath().resolve("testCases");
        File[] children = p.toFile().listFiles(File::isDirectory);

        if (children != null) {
            for (File childFile : children) {
                Directory childTree = mapSuite(childFile);
                node.add(buildTestCasesSubTree(childTree));
            }
        }
        return node;
    }

    public static DefaultMutableTreeNode buildTestPlansSubTree(final Directory project) {
        System.out.println("ExplorerTree.buildTestPlansSubTree()");

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(project);
        Path p = project.getFilePath().resolve("testPlans");
        File[] children = p.toFile().listFiles(File::isDirectory);

        if (children != null) {
            Arrays.stream(children).forEach(file -> System.out.println(file.getName()));

            for (File childFile : children) {
                Directory childTree = mapSuite(childFile);
                node.add(buildTestPlansSubTree(childTree));
            }
        }
        return node;
    }


    /**
     * Map file to Project Directory
     * Expected format: 0_100_ProjectName_1
     * Type_Id_Name_Active
     */
    @Nullable
    public static Directory mapProject(@NotNull final File file) {
        System.out.println("DirectoryMapper.mapProject(): " + file.getName());
        String[] parts = file.getName().split("_", 4);

        return new Directory()
                .setFile(file)
                .setFilePath(file.toPath())
                .setFileName(file.getName())
                .setType(Integer.parseInt(parts[0]))
                .setId(Integer.parseInt(parts[1]))
                .setName(parts[2])
                .setActive(Integer.parseInt(parts[3]));
    }

    /**
     * Map file to Suite/Feature Directory
     * Expected format: 1_200_SuiteName or 2_300_FeatureName
     * Type_Id_Name
     */
    @Nullable
    public static Directory mapSuite(@NotNull final File file) {
        System.out.println("DirectoryMapper.mapSuite(): " + file.getName());

        String[] parts = file.getName().split("_", 3);

        return new Directory()
                .setFile(file)
                .setFilePath(file.toPath())
                .setFileName(file.getName())
                .setType(Integer.parseInt(parts[0]))
                .setId(Integer.parseInt(parts[1]))
                .setName(parts[2]);
    }


    private static class ReloadAllOnInsertListener implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            System.out.println("ExplorerTree.treeNodesChanged()");
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            System.out.println("ExplorerTree.treeNodesInserted()");
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            System.out.println("ExplorerTree.treeNodesRemoved()");

        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            System.out.println("ExplorerTree.treeStructureChanged()");

        }

    }
}