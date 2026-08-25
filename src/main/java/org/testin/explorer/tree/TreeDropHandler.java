package org.testin.explorer.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FileDropEvent;
import com.intellij.openapi.editor.FileDropHandler;
import com.intellij.openapi.project.Project;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.EditorUtil;

import java.awt.datatransfer.Transferable;

public class TreeDropHandler implements FileDropHandler {

    /**
     * The platform's own signature: a Kotlin suspend function seen from Java,
     * whose return is either the result or the suspension marker. Nullable
     * because the platform declares it so (#71).
     */
    @Override
    public @Nullable Object handleDrop(final @NotNull FileDropEvent event, final @NotNull Continuation<? super Boolean> continuation) {
        final @NotNull Project p = event.getProject();
        final @NotNull Transferable transferable = event.getTransferable();

        if (!transferable.isDataFlavorSupported(TreeTransferHandler.NODE_FLAVOR)) {
            return false;
        }

        try {
            final @NotNull TreeTransferPayload payload = (TreeTransferPayload) transferable.getTransferData(TreeTransferHandler.NODE_FLAVOR);

            ApplicationManager.getApplication().invokeLater(() -> {
                for (final DirectoryDto node : payload.nodes()) {

                    // Asked of the node rather than worked out from its class.
                    // The two kinds that were tested for here are exactly the two
                    // that declare an editor, and both branches did the same
                    // thing - so a third openable kind would have had to be added
                    // here as well as to the declaration, and nothing would have
                    // said so when it was not.
                    if (!node.isOpenableInEditor()) continue;

                    Logger.info("dragged " + node.getType().getDisplayedName() + ": " + node.getName());
                    Services.getInstance(p, EditorUtil.class).open(p, node);
                }
            });
            return true;

        } catch (final Exception ex) {
            Logger.error("Exception: " + ex.getMessage());
            return false;
        }
    }
}
