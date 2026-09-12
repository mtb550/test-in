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
import org.testin.services.Services;
import org.testin.notifications.Notifier;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.util.Bundle;
import org.testin.util.ClipboardContents;

import javax.swing.*;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * UC-TREE-PANEL-013, UC-TREE-PANEL-014.
 * <p>
 * Declared in {@code plugin.xml} (#119), so it has no constructor and no
 * fields: the platform builds one instance for the whole IDE, and the tree it
 * pastes into comes from the keystroke.
 * <p>
 * With no default key, for the reason Copy and Cut give: CTRL+V is the grid's
 * gesture for its own cells as well as the tree's, and a registered shortcut is
 * dispatched before a component's input map - so the tree binds it on this
 * action rather than claiming the key for the whole IDE.
 * <p>
 * The paste itself is in {@link Work}, which is what a keystroke that arrived in
 * a Testin tree with a project behind it has to work with.
 */
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

    /**
     * UC-TREE-PANEL-013.
     * <p>
     * Greyed out where pasting has no meaning: a test project holds its two
     * containers and nothing else, so nothing is ever a child of one.
     * <p>
     * Offered, though, where the nodes on the clipboard cannot land - into the
     * folder they already sit in, or into the wrong family. Those are refusals
     * with a reason, and {@link Work#paste} gives it. Graying them out instead is
     * what made Ctrl+V do nothing at all and say nothing about why.
     * <p>
     * Asked of the transfer handler, which is what the paste itself asks.
     * Deciding it here would be a second rule to keep in step. There is no
     * handler to ask outside the Testin tree, which is what keeps the key gray
     * in a Java file (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeTransferHandler.of(e)
                .filter(TreeTransferHandler::canPasteFromClipboard)
                .isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }

    /**
     * Pasting into one node, for a project and a tree that are both there.
     */
    private record Work(@NotNull Project p, @NotNull TreeTransferHandler transferHandler) {

        // UC-TREE-PANEL-013, Rule-TREE-PANEL-006
        private void paste(final @NotNull Transferable contents, final @NotNull DirectoryDto target) {
            try {
                final @NotNull TreeTransferPayload payload = (TreeTransferPayload) contents.getTransferData(TreeTransferHandler.NODE_FLAVOR);

                // Only what can actually land on this target - family rules, plus
                // never onto itself, into its own subtree, or into its own parent.
                final @NotNull List<DirectoryDto> nodes = Arrays.stream(payload.nodes())
                        .filter(node -> transferHandler.canTransferInto(node, target))
                        .toList();

                final boolean collisionsReported = transferHandler.notifyNameCollisions(payload.nodes(), target);

                if (nodes.isEmpty()) {
                    // Said out loud. Pasting into the folder a node already sits in
                    // is refused - a copy beside itself has no name to take - and
                    // the refusal used to be a silent return, so the tester pressed
                    // Ctrl+V and the tree did not move. Not said twice: a name
                    // collision has already named the nodes it stopped.
                    if (!collisionsReported) {
                        Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("paste.select.folder"));
                    }

                    return;
                }

                // Cut-paste moves, copy-paste duplicates - each says what it does.
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
                        transferHandler::pasteFromClipboard
                ).show();

            } catch (final Exception ex) {
                Logger.error("Paste failed: " + ex.getMessage());
            }
        }
    }

}
