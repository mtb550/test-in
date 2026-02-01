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

public class DirectoryMapper {
    @Getter
    public static DefaultTreeModel treeModel;

    public static void buildTree() {
        System.out.println("ExplorerTree.buildTree()");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("TEST CASES");
        File[] children = Config.getRootFolder().listFiles(File::isDirectory);

        if (children != null) {
            for (File child : children) {
                Directory project = mapProject(child);
                rootNode.add(buildSubTree(project));
            }
        }
        treeModel = new DefaultTreeModel(rootNode);
    }

    public static DefaultMutableTreeNode buildSubTree(final Directory folder) {
        System.out.println("ExplorerTree.buildSubTree()");

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(folder);
        File[] children = folder.getFilePath().toFile().listFiles(File::isDirectory);

        if (children != null) {
            for (File childFile : children) {
                Directory childTree = mapSuite(childFile);
                node.add(buildSubTree(childTree));
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