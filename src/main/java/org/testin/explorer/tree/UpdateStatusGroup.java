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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.model.NodeStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.Marker;

import java.util.List;

/**
 * Every status the selected node can be set to, as one thing the platform owns
 * (#119).
 * <p>
 * The entries are generated twice over: from the node, which says which statuses
 * it has, and from that enum's {@code values()}, which says what they are. So
 * the enum stays the one place a status is declared - a fourth constant appears
 * in the menu with nothing else to change - and there is one group rather than
 * one per kind of node, which is what there was (#110).
 * <p>
 * Compact rather than a submenu - {@code popup="false"} in the descriptor - so
 * the statuses sit directly in the Actions menu where they always have.
 */
public class UpdateStatusGroup extends DefaultActionGroup {

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
        if (e == null) return EMPTY_ARRAY;

        return statuses(e).stream()
                .map(UpdateStatusAction::new)
                .toArray(AnAction[]::new);
    }

    private static @NotNull List<NodeStatus> statuses(final @NotNull AnActionEvent e) {
        return TestinData.singleSelectedNode(e)
                .map(DirectoryDto::getMarker)
                .map(Marker::statuses)
                .orElseGet(List::of);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
