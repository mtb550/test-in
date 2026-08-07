package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FileDropEvent;
import com.intellij.openapi.editor.FileDropHandler;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.util.EditorUtil;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.Transferable;

public class TreeDropHandler implements FileDropHandler {

    @Override
    public @Nullable Object handleDrop(final @NotNull FileDropEvent event, final @NotNull Continuation<? super Boolean> continuation) {
        final @NotNull Project p = event.getProject();
        final Transferable transferable = event.getTransferable();

        if (!transferable.isDataFlavorSupported(TreeTransferHandler.NODE_FLAVOR)) {
            return false;
        }

        try {
            final DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) transferable.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            ApplicationManager.getApplication().invokeLater(() -> {
                for (DefaultMutableTreeNode node : nodes) {

                    if (node.getUserObject() instanceof TestSetDirectoryDto ts) {
                        Logger.info("dragged Test set: " + ts.getName());

                        Services.getInstance(p, EditorUtil.class).openIfNotOpen(p, ts);
                        continue;
                    }

                    if (node.getUserObject() instanceof TestRunDirectoryDto tr) {
                        Logger.info("dragged Test Run: " + tr.getName());
                        Services.getInstance(p, EditorUtil.class).openIfNotOpen(p, tr);
                    }
                }
            });
            return true;

        } catch (final Exception ex) {
            Logger.error("Exception: " + ex.getMessage());
            return false;
        }
    }
}