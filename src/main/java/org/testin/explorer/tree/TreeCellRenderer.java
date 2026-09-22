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

import javax.swing.JTree;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
public class TreeCellRenderer extends ColoredTreeCellRenderer {
    private final @NotNull Set<DirectoryDto> selectedNodes;

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-099
    private static @NotNull String statusLabel(final @NotNull DirectoryDto dir) {
        if (dir instanceof TestRunDirectoryDto) return dir.getMarker().getStatusLabel();

        return dir.getMarker().status().isActive() ? "" : dir.getMarker().getStatusLabel();
    }

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

            final @NotNull Optional<TestRunStatus> runStatus = dir instanceof TestRunDirectoryDto trDir
                    ? Optional.of(trDir.getMarker().getStatus())
                    : Optional.empty();

            setIcon(runStatus.map(TestRunStatus::getIcon).orElseGet(type::getIcon));
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
}
