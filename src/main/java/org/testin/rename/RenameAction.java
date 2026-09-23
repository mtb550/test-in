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

package org.testin.rename;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditors;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Optional;

// UC-TREE-PANEL-011
public class RenameAction extends DumbAwareAction {
    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104, Rule-TREE-PANEL-111
    private static @NotNull Optional<String> whyNot(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        if (!dir.isRenamable()) return Optional.of(Bundle.message("rename.disabled.description"));
        if (Services.getInstance(p, TestinEditors.class).busyUnder(p, dir))
            return Optional.of(Bundle.message("rename.disabled.busy"));

        return Optional.empty();
    }

    // UC-TREE-PANEL-011
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull Optional<Project> project = Optional.ofNullable(e.getProject());
        if (project.isEmpty()) return;

        final @NotNull Project p = project.orElseThrow();

        TestinData.singleSelectedNode(e)
                .filter(dir -> whyNot(p, dir).isEmpty())
                .ifPresent(dir -> new RenameDialog(p, dir, newName -> renameNode(p, dir, newName)).show());
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-004
    private void renameNode(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        if (newName.isBlank() || newName.equals(dir.getName())) return;

        if (Optional.ofNullable(dir.getPath().getParent()).isEmpty()) {
            Logger.warn("Rename refused, no parent directory: " + dir.getPath());
            return;
        }

        if (NodeRename.refused(p, dir, newName)) return;

        final @NotNull String oldName = dir.getName();
        final @NotNull Path oldPath = dir.getPath();
        final @NotNull Path newPath = oldPath.resolveSibling(newName);
        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);

        NodeRename.apply(p, tp, dir, newName, () -> {
            Services.getInstance(p, Notifier.class).softShow(p, Done.RENAMED);

            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    Bundle.message("rename.undo", oldName),
                    () -> applyRename(p, newPath, oldName),
                    () -> applyRename(p, oldPath, newName),
                    () -> {
                    }));
        });
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-037
    private boolean applyRename(final @NotNull Project p, final @NotNull Path path, final @NotNull String newName) {
        final @NotNull Optional<DirectoryDto> node = Services.getInstance(p, ProjectIndexer.class).find(path);
        if (node.isEmpty()) {
            Logger.warn("Nothing to rename at " + path + ", so the step is refused");
            return false;
        }

        final @NotNull DirectoryDto dir = node.orElseThrow();
        if (NodeRename.refused(p, dir, newName)) return false;

        NodeRename.apply(p, Services.getInstance(p, TreePanel.class), dir, newName, () -> {
        });
        return true;
    }

    // UC-TREE-PANEL-011, Rule-TREE-PANEL-104
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<String> why = Optional.ofNullable(e.getProject())
                .flatMap(p -> TestinData.singleSelectedNode(e).map(dir -> whyNot(p, dir)))
                .orElse(Optional.of(Bundle.message("rename.disabled.description")));

        GrayWithReason.unless(this, e, why);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
