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
                        System.out.println("dragged Test set: " + ts.getName());
                        EditorUtil.getInstance().openTestSetEditorIfNotOpen(ts);
                        continue;
                    }

                    if (node.getUserObject() instanceof TestRunDirectoryDto tr) {
                        System.out.println("dragged Test Run: " + tr.getName());
                        // todo, hook up opening test run here
                        //continue;
                    }
                }
            });
            return true;

        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
    }
}