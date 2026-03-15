package testGit.util;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import testGit.pojo.TestPackage;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class TreeUtilImpl {
    public static void executeVfsAction(Path path, String errorTitle, VfsOperation operation) {
        WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
                if (vf != null) {
                    operation.execute(vf); // تنفيذ الكود المُمرر
                } else {
                    Messages.showErrorDialog("Could not find path on disk:\n" + path, errorTitle);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Operation failed: " + ex.getMessage(), errorTitle);
            }
        });
    }

    public static DefaultMutableTreeNode insertNode(final SimpleTree tree, final DefaultMutableTreeNode parentNode, final TestPackage newTestPackage) {

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newTestPackage);

        model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

        TreeUtil.selectNode(tree, newNode);

        return newNode;
    }

    // -------------------------------------------------------------------------
    // دوال إدارة واجهة الشجرة (UI Tree Operations)
    // -------------------------------------------------------------------------

    public static void removeNode(DefaultMutableTreeNode node, final SimpleTree tree) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.removeNodeFromParent(node);
    }

    public static void insertVf(final Object requester, final Path parentPath, final String folderName) {
        WriteAction.run(() -> {
            try {
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);
                if (parentVf != null && parentVf.isDirectory()) {
                    parentVf.createChildDirectory(requester, folderName);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not create directory: " + ex.getMessage(), "Error");
            }
        });
    }

    // -------------------------------------------------------------------------
    // دوال نظام الملفات بعد تطبيق المركزية (أنظف بكثير!)
    // -------------------------------------------------------------------------

    public static void removeVf(final Object requester, final File path) {
        WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path);
                if (vf != null) {
                    vf.delete(requester);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not delete file: " + ex.getMessage(), "Error");
            }
        });
    }

    public interface VfsOperation {
        void execute(VirtualFile vf) throws IOException;
    }
}