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

package org.testin.explorer.tree;

import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Rule-TREE-PANEL-105.
 * <p>
 * Transferable for tree drag-and-drop and clipboard: carries the node payload,
 * and nothing another application can read.
 * <p>
 * It also offered the nodes' folders as a file list, so a test set dropped on a
 * file manager was moved there by the file manager - out of the data root,
 * behind the indexer, and with the tree still drawing it. Test data is moved by
 * the indexer alone (#312, A67).
 */
record NodesTransferable(@NotNull TreeTransferPayload payload) implements Transferable {

    @Override
    public @NotNull DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{TreeTransferHandler.NODE_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
        return TreeTransferHandler.NODE_FLAVOR.equals(flavor);
    }

    /**
     * The {@code throws} stays deliberately, and is one of the exceptions CLAUDE.md
     * lists to the rule that a method handles its own failures. AWT's
     * {@link java.awt.datatransfer.Transferable} contract is that an unsupported
     * flavor throws; catching it here would hand the platform's drag-and-drop a
     * wrong object instead of telling it the flavor is not available (#63).
     */
    @Override
    public @NotNull Object getTransferData(final @NotNull DataFlavor flavor) throws UnsupportedFlavorException {
        if (TreeTransferHandler.NODE_FLAVOR.equals(flavor)) return payload;
        throw new UnsupportedFlavorException(flavor);
    }
}
