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

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.DirectoryType;
import org.testin.model.TestRunStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Bundle;

import java.util.Objects;
import javax.swing.*;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public class TreeCellRenderer extends ColoredTreeCellRenderer {
    /**
     * Shared with the transfer handler: the nodes currently cut, drawn grayed.
     */
    private final @NotNull Set<DirectoryDto> selectedNodes;

    // Rule-TREE-PANEL-008
    @Override
    public void customizeCellRenderer(final @NotNull JTree tree, final @Nullable Object value, final boolean selected, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
        try {
            if (TreeValues.valueOf(value) instanceof TreeLoadError(String message)) {
                setIcon(AllIcons.General.Error);
                append(message, SimpleTextAttributes.ERROR_ATTRIBUTES);
                return;
            }
            if (!(TreeValues.valueOf(value) instanceof DirectoryDto dir)) {
                append(Objects.toString(value, ""), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                return;
            }
            final @NotNull DirectoryType type = dir.getType();

            // Only a run has one; every other node is drawn as the kind it is.
            final @NotNull Optional<TestRunStatus> runStatus = dir instanceof TestRunDirectoryDto trDir
                    ? Optional.of(trDir.getMarker().getStatus())
                    : Optional.empty();

            // A run is drawn as its status, not as its kind: the tree then says
            // where every cycle stands without opening any of them. Every other
            // node takes the icon of what it is.
            setIcon(runStatus.map(TestRunStatus::getIcon).orElseGet(type::getIcon));
            // Gray for a node that is cut, and for one retired from current work -
            // a deprecated test set or an archived package - so the tree says at
            // a glance what is live without opening the details of anything.
            final boolean grayed = selectedNodes.contains(dir) || dir.isRetired();
            append(dir.getName(), grayed ? SimpleTextAttributes.GRAYED_ATTRIBUTES : type.getAttributes());
            append(" ");
            append(statusLabel(dir), SimpleTextAttributes.GRAY_ATTRIBUTES);

        } catch (final Exception ex) {
            Logger.error("Error rendering tree node: " + ex.getMessage());
            setIcon(AllIcons.General.Error);
            append(Objects.toString(value, Bundle.message("tree.render.error")), SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }


    /**
     * UC-TREE-PANEL-001, Rule-TREE-PANEL-099.
     * <p>
     * The word beside the name: what this node's status is, in gray, and nothing
     * for a node with nothing to say.
     * <p>
     * <b>A run always says where it stands</b>, because that is what a run is
     * for - a cycle's state is the first thing anyone wants from the tree.
     * <p>
     * <b>Every other node says it only when it is not active.</b> An inactive
     * test project, a deprecated test set and an archived package each say which
     * they are; a node in current work says nothing, because "Active" beside
     * every name is a word the tester reads a hundred times and needed never.
     * <p>
     * Not the same question as {@code isRetired()}, which this asked first and
     * which is a proxy: retired means gray, sorted last and left collapsed, and
     * an inactive test project is none of those - it is simply not the one being
     * worked on.
     * <p>
     * Asked of the marker rather than of the kind. Every marker answers
     * {@code getStatusLabel()} - it is one line on {@code Marker} since #110 -
     * so this reads the same sentence for a run, a package and a test set
     * instead of knowing which enum each of them carries (#66, finding 7).
     */
    private static @NotNull String statusLabel(final @NotNull DirectoryDto dir) {
        if (dir instanceof TestRunDirectoryDto) return dir.getMarker().getStatusLabel();

        return dir.getMarker().status().isActive() ? "" : dir.getMarker().getStatusLabel();
    }

    // todo, if (dir instanceof TestSetDirectoryDto setDir) {
    // todo, later, make a tag for test set if it is approved or still, need to set business and plan before implement
}

