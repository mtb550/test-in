package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FileDropEvent;
import com.intellij.openapi.editor.FileDropHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.EditorUtil;
import org.testin.util.logger.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.Transferable;

public class TreeDropHandler implements FileDropHandler {

    private static @NotNull Project getProject() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 0) {
            return openProjects[0];
        }
        throw new IllegalStateException("No open project found");
    }

    @Override
    public @Nullable Object handleDrop(final @NotNull FileDropEvent event, final @NotNull Continuation<? super Boolean> continuation) {
        final Transferable transferable = event.getTransferable();

        if (!transferable.isDataFlavorSupported(TreeTransferHandler.NODE_FLAVOR)) {
            return false;
        }

        try {
            final DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) transferable.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            ApplicationManager.getApplication().invokeLater(() -> {
                for (DefaultMutableTreeNode node : nodes) {

                    if (node.getUserObject() instanceof TestSetDirectoryDto ts) {
                        Log.info("dragged Test set: " + ts.getName());
                        Project project = getProject();
                        EditorUtil.getInstance().openEditorIfNotOpen(project, ts);
                        continue;
                    }

                    if (node.getUserObject() instanceof TestRunDirectoryDto tr) {
                        Log.info("dragged Test Run: " + tr.getName());
                        Project project = getProject();
                        EditorUtil.getInstance().openEditorIfNotOpen(project, tr);
                    }
                }
            });
            return true;

        } catch (Exception e) {
            Log.error("Exception: " + e.getMessage());
            return false;
        }
    }
}