package testGit.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Path;

public class TreeUtilImpl {
    public static void executeVfsAction(Path path, String errorTitle, VfsOperation operation) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
                if (vf != null) {
                    operation.execute(vf);
                } else {
                    Messages.showErrorDialog("Could not find path on disk:\n" + path, errorTitle);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Operation failed: " + ex.getMessage(), errorTitle);
            }
        }));
    }

    public static void executeVfsAction(Path sourcePath, Path targetPath, String errorTitle, VfsBiOperation operation) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
                VirtualFile targetVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath);

                if (sourceVf != null && targetVf != null) {
                    operation.execute(sourceVf, targetVf);
                } else {
                    Messages.showErrorDialog("Could not find source or target path on disk.", errorTitle);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Operation failed: " + ex.getMessage(), errorTitle);
            }
        }));
    }

    public static void createNode(final SimpleTree tree, final DefaultMutableTreeNode parentNode, final Object newTestPackage) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newTestPackage);

        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
            TreeUtil.selectNode(tree, newNode);
        });

    }

    public static void removeNode(DefaultMutableTreeNode node, final SimpleTree tree) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(node);
        });
    }

    public static void createVf(final Object requester, final Path parentPath, final String folderName) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);
                if (parentVf != null && parentVf.isDirectory()) {
                    parentVf.createChildDirectory(requester, folderName);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not create directory: " + ex.getMessage(), "Error");
            }
        }));
    }

    public static void createDataVf(final Object requester, final Path parentPath, final String fileName) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);

                if (parentVf != null && parentVf.isDirectory()) {
                    if (parentVf.findChild(fileName) == null) {
                        parentVf.createChildData(requester, fileName);
                    }
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not create marker file: " + ex.getMessage(), "Error");
            }
        }));
    }

    public static void removeVf(final Object requester, final Path path) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
                if (vf != null) {
                    vf.delete(requester);
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Could not delete file: " + ex.getMessage(), "Error");
            }
        }));
    }

    public interface VfsOperation {
        void execute(VirtualFile vf) throws IOException;
    }

    public interface VfsBiOperation {
        void execute(VirtualFile sourceVf, VirtualFile targetVf) throws IOException;
    }
}