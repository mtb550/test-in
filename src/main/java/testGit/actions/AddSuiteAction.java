package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Directory;
import testGit.util.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;

public class AddSuiteAction extends AnAction {
    private final SimpleTree tree;

    public AddSuiteAction(final SimpleTree tree) {
        super("➕ New Suite");
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == NodeType.FEATURE.getCode()) return;

        String name = Messages.showInputDialog("Enter suite name:", "Add Suite", null);
        if (name == null || name.isBlank()) return;

        Directory newSuite = new Directory().setType(NodeType.SUITE.getCode()).setId(50).setName(name);
        newSuite.setFileName(newSuite.getType() + "_" + newSuite.getId() + "_" + newSuite.getName());
        newSuite.setFilePath(treeItem.getFilePath().resolve(newSuite.getFileName()));
        newSuite.setFile(new File(newSuite.getFileName()));

        WriteAction.run(() -> {
            try {
                // 1. Get the parent directory as a VirtualFile
                VirtualFile parentDir = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(treeItem.getFilePath());

                if (parentDir != null) {
                    // 2. Create the directory using IntelliJ's API
                    // This triggers VFS events correctly and prevents the "watcher" clash
                    VirtualFile newDir = parentDir.createChildDirectory(this, newSuite.getFileName());

                    // 3. Update your Tree Model immediately inside the same WriteAction
                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newSuite);
                    model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

                    // 4. Ensure UI visibility
                    tree.scrollPathToVisible(new TreePath(newNode.getPath()));
                }
            } catch (IOException ex) {
                System.err.println("unable to create suite: " + newSuite);
            }
        });

    }
}