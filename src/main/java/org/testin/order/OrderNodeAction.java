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

package org.testin.order;

import org.testin.actions.GrayWithReason;
import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.util.Optional;

public class OrderNodeAction extends DumbAwareAction {
    // UC-TREE-PANEL-015
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        orderable(e).ifPresent(node -> new OrderDialog(p, node.getOrder(), order -> apply(p, node, order)).show());
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    private void apply(final @NotNull Project p, final @NotNull DirectoryDto node, final int order) {
        if (node.getOrder() == order) return;

        final int before = node.getOrder();
        if (!place(p, node, order)) return;

        Services.getInstance(p, Notifier.class).softShow(p, Done.ORDERED);

        // Rule-TREE-PANEL-103
        Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                Bundle.message("order.undo", node.getName()),
                () -> place(p, node, before),
                () -> place(p, node, order),
                () -> {
                }));
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-055
    private boolean place(final @NotNull Project p, final @NotNull DirectoryDto node, final int order) {
        final int was = node.getOrder();
        node.getMarker().setOrder(order);

        if (!Services.getInstance(p, ProjectIndexer.class).persistMarker(node)) {
            node.getMarker().setOrder(was);
            return false;
        }

        Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
        return true;
    }

    private @NotNull Optional<DirectoryDto> orderable(final @NotNull AnActionEvent e) {
        return TestinData.singleSelectedNode(e).filter(DirectoryDto::isOrderable);
    }

    // UC-TREE-PANEL-015, Rule-TREE-PANEL-058
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, orderable(e).isPresent(), Bundle.message("order.disabled.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
