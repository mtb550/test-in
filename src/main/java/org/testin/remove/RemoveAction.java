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

package org.testin.remove;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.Declared;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.indexer.NodeCounter;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.ui.framework.ConfirmDialog;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

// UC-TREE-PANEL-012
public class RemoveAction extends DumbAwareAction {
    private static @NotNull List<DirectoryDto> removableNodes(final @NotNull AnActionEvent e) {
        return TestinData.selectedNodes(e).stream()
                .filter(DirectoryDto::isRemovable)
                .toList();
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<DirectoryDto> nodesToRemove = removableNodes(e);
        if (nodesToRemove.isEmpty()) return;

        new Work(p).confirm(nodesToRemove);
    }

    // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
    @Override
    public void update(final @NotNull AnActionEvent e) {
        GrayWithReason.unless(this, e, !removableNodes(e).isEmpty(), Bundle.message("remove.node.disabled.description"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p) {
        // UC-TREE-PANEL-012, Rule-TREE-PANEL-038
        private void confirm(final @NotNull List<DirectoryDto> nodesToRemove) {
            final @NotNull String holds = nodesToRemove.size() == 1
                    ? NodeCounter.childCounts(p, nodesToRemove.getFirst()).describe()
                    : "";

            final @NotNull String msg = (nodesToRemove.size() == 1
                    ? Bundle.message("remove.confirm.one", nodesToRemove.getFirst().getName())
                    : Bundle.message("remove.confirm.many", String.valueOf(nodesToRemove.size())))
                    + (holds.isEmpty() ? "" : System.lineSeparator() + holds);

            final @NotNull String from = nodesToRemove.size() == 1 ? nodesToRemove.getFirst().getPath().toString() : "";
            new ConfirmDialog(p, Bundle.message("remove.confirm.title"), msg, from, "", Bundle.message("remove.confirm.button"), () -> removeNodes(nodesToRemove)).show();
        }

        // UC-TREE-PANEL-012, Rule-TREE-PANEL-041
        private void removeNodes(final @NotNull List<DirectoryDto> nodesToRemove) {
            if (nodesToRemove.isEmpty()) return;

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull List<Kept> kept = new ArrayList<>(nodesToRemove.size());

            // Rule-TREE-PANEL-102
            final boolean copied = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                for (final DirectoryDto node : nodesToRemove) {
                    ProgressManager.checkCanceled();
                    indexer.keepAside(node.getPath()).ifPresent(copy -> kept.add(new Kept(node, node.getPath(), copy)));
                }
            }, Bundle.message("remove.progress"), true, p);

            if (!copied) {
                kept.forEach(one -> indexer.forgetKept(one.copy()));
                return;
            }

            // Rule-TREE-PANEL-116
            for (final DirectoryDto node : nodesToRemove) {
                Services.getInstance(p, TestinEditors.class).close(p, node);
            }

            removeEach(nodesToRemove, went -> {
                Logger.info("Removed " + went.size() + " of " + nodesToRemove.size() + " node(s).");

                final @NotNull List<Kept> undoable = kept.stream().filter(one -> went.contains(one.dto())).toList();
                kept.stream().filter(one -> !went.contains(one.dto())).forEach(one -> indexer.forgetKept(one.copy()));

                if (went.isEmpty()) return;

                recordRemoval(went, undoable);
                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.REMOVED, went.size());

                final int lost = went.size() - undoable.size();
                if (lost > 0) {
                    Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.not.undoable.title"),
                            lost == 1
                                    ? Bundle.message("remove.not.undoable.one", Declared.shortcutText(IdeActions.ACTION_UNDO))
                                    : Bundle.message("remove.not.undoable.many", String.valueOf(lost), Declared.shortcutText(IdeActions.ACTION_UNDO)));
                }
            });
        }

        // UC-TREE-PANEL-012, Rule-TREE-PANEL-042
        private void removeEach(final @NotNull List<DirectoryDto> nodes, final @NotNull Consumer<@NotNull List<DirectoryDto>> whenAllGone) {
            final @NotNull AtomicInteger pending = new AtomicInteger(nodes.size());
            final @NotNull List<DirectoryDto> went = new CopyOnWriteArrayList<>();

            for (final DirectoryDto node : nodes) {
                Removals.of(node.getType()).remove(p, node, wasRemoved -> {
                    if (wasRemoved) went.add(node);
                    if (pending.decrementAndGet() != 0) return;

                    Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();
                    whenAllGone.accept(List.copyOf(went));
                });
            }
        }

        // UC-TREE-PANEL-012, Rule-TREE-PANEL-040
        private void recordRemoval(final @NotNull List<DirectoryDto> asked, final @NotNull List<Kept> kept) {
            final @NotNull String what = asked.size() == 1
                    ? Bundle.message("remove.undo.one", asked.getFirst().getName())
                    : Bundle.message("remove.undo.many", String.valueOf(asked.size()));

            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    what,
                    () -> restoreAll(kept),
                    () -> {
                        removeAll(kept);
                        return true;
                    },
                    () -> kept.forEach(one -> Services.getInstance(p, ProjectIndexer.class).forgetKept(one.copy()))));
        }

        // UC-TREE-PANEL-016, Rule-TREE-PANEL-040, Rule-INTERNAL-063
        private boolean restoreAll(final @NotNull List<Kept> kept) {
            if (kept.isEmpty()) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.not.undoable.title"),
                        Bundle.message("remove.nothing.kept"));
                return false;
            }

            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            // Rule-TREE-PANEL-102
            final @NotNull List<Kept> lost = new ArrayList<>();
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> lost.addAll(kept.stream().filter(one -> !indexer.restoreNode(one.copy(), one.original())).toList()),
                    Bundle.message("remove.undo.progress"), false, p);
            Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();

            if (lost.isEmpty()) return true;

            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("remove.undo.incomplete.title"), Bundle.message("remove.undo.incomplete.message", String.valueOf(lost.size()), String.valueOf(kept.size())));
            return false;
        }

        // UC-TREE-PANEL-017
        private void removeAll(final @NotNull List<Kept> kept) {
            removeEach(kept.stream().map(Kept::dto).toList(), went -> Logger.info("Removed " + went.size() + " node(s) again."));
        }
    }

    private record Kept(@NotNull DirectoryDto dto, @NotNull Path original, @NotNull Path copy) {
    }
}
