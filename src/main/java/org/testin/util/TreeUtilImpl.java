package org.testin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.util.notifications.Notifier;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.nio.file.Path;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Service(Service.Level.PROJECT)
public final class TreeUtilImpl {

    public void executeVfsAction(final @NotNull Project project, final @NotNull Path path, final @NotNull String errorTitle, final @NotNull IVfsOperation operation) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
                if (vf != null) {
                    operation.execute(vf);
                } else {
                    Services.getInstance(project, Notifier.class).error(project, "Could not find path on disk:\n" + path, errorTitle);
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class).error(project, "Operation failed: " + ex.getMessage(), errorTitle);
            }
        }));
    }

    public void executeVfsAction(final @NotNull Project project, final @NotNull Path sourcePath, final @NotNull Path targetPath, final @NotNull String errorTitle, final @NotNull IVfsBiOperation operation) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile sourceVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
                VirtualFile targetVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath);

                if (sourceVf != null && targetVf != null) {
                    operation.execute(sourceVf, targetVf);
                } else {
                    Services.getInstance(project, Notifier.class).error(project, "Could not find source or target path on disk.", errorTitle);
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class).error(project, "Operation failed: " + ex.getMessage(), errorTitle);
            }
        }));
    }

    public void createNode(final SimpleTree tree, final DefaultMutableTreeNode parentNode, final Object newTestPackage) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newTestPackage);

        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
            TreeUtil.selectNode(tree, newNode);
        });
    }

    public void removeNode(final DefaultMutableTreeNode node, final SimpleTree tree) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            if (node.getParent() != null) {
                model.removeNodeFromParent(node);
            }
        });
    }

    public void removeRootNode(final SimpleTree tree) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.setRoot(null);
        });
    }

    public void createVf(final @NotNull Project project, final Object requester, final Path parentPath, final String folderName) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);
                if (parentVf != null && parentVf.isDirectory()) {
                    parentVf.createChildDirectory(requester, folderName);
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class).error(project, "Could not create directory: " + ex.getMessage(), "Error");
            }
        }));
    }

    public void createDataVf(final @NotNull Project project, final Object requester, final Path parentPath, final String fileName) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile parentVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(parentPath);

                if (parentVf != null && parentVf.isDirectory()) {
                    if (parentVf.findChild(fileName) == null) {
                        parentVf.createChildData(requester, fileName);
                    }
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class).error(project, "Could not create marker file: " + ex.getMessage(), "Error");
            }
        }));
    }

    public void removeVf(final @NotNull Project project, final Object requester, final Path path) {
        ApplicationManager.getApplication().invokeLater(() -> WriteAction.run(() -> {
            try {
                VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
                if (vf != null) {
                    vf.delete(requester);
                }
            } catch (IOException ex) {
                Services.getInstance(project, Notifier.class).error(project, "Could not delete file: " + ex.getMessage(), "Error");
            }
        }));
    }

    public interface IVfsOperation {
        void execute(VirtualFile vf) throws IOException;
    }

    public interface IVfsBiOperation {
        void execute(VirtualFile sourceVf, VirtualFile targetVf) throws IOException;
    }
}