package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FileDropEvent;
import com.intellij.openapi.editor.FileDropHandler;
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
                        EditorUtil.getInstance().openEditorIfNotOpen(ts);
                        continue;
                    }

                    if (node.getUserObject() instanceof TestRunDirectoryDto tr) {
                        Log.info("dragged Test Run: " + tr.getName());
                        EditorUtil.getInstance().openEditorIfNotOpen(tr);
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