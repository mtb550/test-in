/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.clipboard;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.tree.TreeTransferHandler;
import org.testin.explorer.tree.TreeTransferPayload;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;
import org.testin.util.FailureText;

import javax.swing.TransferHandler;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// UC-TREE-PANEL-013, UC-TREE-PANEL-014
public class PasteNodeAction extends DumbAwareAction {
    // UC-TREE-PANEL-013, UC-TREE-PANEL-014
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TreeTransferHandler.of(e).ifPresent(handler ->
                ClipboardContents.withFlavor(TreeTransferHandler.NODE_FLAVOR).ifPresent(contents ->
                        TestinData.singleSelectedNode(e).ifPresent(target -> new Work(p, handler).paste(contents, target))));
    }

    // UC-TREE-PANEL-013
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeTransferHandler.of(e)
                .filter(TreeTransferHandler::canPasteFromClipboard)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p, @NotNull TreeTransferHandler transferHandler) {
        // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
        private void paste(final @NotNull Transferable contents, final @NotNull DirectoryDto target) {
            try {
                final @NotNull TreeTransferPayload payload = (TreeTransferPayload) contents.getTransferData(TreeTransferHandler.NODE_FLAVOR);

                final @NotNull List<DirectoryDto> nodes = Arrays.stream(payload.nodes())
                        .filter(node -> transferHandler.canTransferInto(node, target))
                        .toList();

                final boolean collisionsReported = transferHandler.notifyNameCollisions(payload.nodes(), target);

                if (nodes.isEmpty()) {
                    if (!collisionsReported) {
                        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("paste.select.folder"));
                    }

                    return;
                }

                final boolean move = payload.clipboardAction() == TransferHandler.MOVE;
                final @NotNull String verb = move ? Bundle.message("transfer.move") : Bundle.message("transfer.copy");
                final @NotNull String what = nodes.size() == 1
                        ? "'" + nodes.getFirst().getName() + "'"
                        : Bundle.message("transfer.items", String.valueOf(nodes.size()));
                final @NotNull Path fromPath = nodes.getFirst().getPath().getParent();

                new ConfirmDialog(p, Bundle.message("paste.title"),
                        Bundle.message("transfer.confirm", verb, what, target.getName()),
                        Objects.toString(fromPath, ""),
                        target.getPath().toString(),
                        verb,
                        () -> transferHandler.pasteFromClipboard(target)
                ).show();

            } catch (final Exception ex) {
                Logger.error("Paste Node failed: " + FailureText.of(ex));
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("clipboard.paste.failed.title"), FailureText.of(ex));
            }
        }
    }
}
