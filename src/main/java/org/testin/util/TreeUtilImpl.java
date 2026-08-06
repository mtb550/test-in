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
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
            if (vf != null) {
                operation.execute(vf);
            } else {
                Services.getInstance(project, Notifier.class).error(project, "Could not find path on disk:\n" + path, errorTitle);
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
            } catch (final Exception ex) {
                Services.getInstance(project, Notifier.class).error(project, "Operation failed: " + ex.getMessage(), errorTitle);
            }
        }));
    }

    public void createNode(final SimpleTree tree, final DefaultMutableTreeNode node, final Object obj) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(obj);

        ApplicationManager.getApplication().invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(newNode, node, node.getChildCount());
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

    public void removeVf(final @NotNull Project project, final Object requester, final Path path) {
        try {
            VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(path.toFile());
            if (vf != null) {
                WriteAction.run(() -> vf.delete(requester));
            }
        } catch (final IOException ex) {
            Services.getInstance(project, Notifier.class).error(project, "Could not delete file: " + ex.getMessage(), "Error");
        }
    }

    public interface IVfsOperation {
        void execute(VirtualFile vf);
    }

    public interface IVfsBiOperation {
        void execute(VirtualFile sourceVf, VirtualFile targetVf);
    }
}