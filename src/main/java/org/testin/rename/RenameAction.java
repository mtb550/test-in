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

import org.testin.codegen.CodeOn;
import org.testin.actions.GrayWithReason;
import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.project.DumbAwareAction;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.undo.UndoScope;
import org.testin.undo.UndoHistories;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.codegen.JavaCode;
import org.testin.codegen.Renamed;
import org.testin.editor.TestinEditors;
import com.intellij.openapi.project.DumbService;
import org.testin.util.Bundle;

import java.util.Optional;

/**
 * UC-TREE-PANEL-011.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and makes its key remappable in Settings -> Keymap. That is why it has no
 * constructor and no fields: the platform builds one instance for the whole
 * IDE, so what it acts on has to come from the keystroke rather than from
 * whoever built it.
 * <p>
 * Its name, description, icon and default key are in the XML for the same
 * reason - the Keymap page reads them from there, and a second copy in a
 * {@code super(...)} call is one of them going stale.
 */
public class RenameAction extends DumbAwareAction {

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

        // No parent means a filesystem root, which is not a node this tree can
        // rename. Asked first because applyRename resolves the new path against
        // the parent and would throw on null (#66, F3).
        if (Optional.ofNullable(dir.getPath().getParent()).isEmpty()) {
            Logger.warn("Rename refused, no parent directory: " + dir.getPath());
            return;
        }

        if (refused(p, dir, newName)) return;

        final @NotNull String oldName = dir.getName();
        final @NotNull TreePanel tp = Services.getInstance(p, TreePanel.class);

        // Both inside the callback. NodeRename.apply's onDone runs when the
        // rename has finished and never if it failed, and the entry used to be
        // pushed on the line after it regardless - so a tester who saw "Rename
        // Failed" then found CTRL+Z offering Undo Rename, which renamed the node
        // to the name it already had and reported Undone, while the operation
        // they wanted back was one press further down (#66, finding 74).
        NodeRename.apply(p, tp, dir, newName, () -> {
            Services.getInstance(p, Notifier.class).softShow(p, Done.RENAMED);

            // The dto reference stays valid across renames, so undo and redo are
            // the same routine with the names swapped.
            Services.getInstance(p, UndoHistories.class).push(UndoScope.TREE, new UndoHistories.Operation(
                    Bundle.message("rename.undo", oldName),
                    () -> applyRename(p, dir, oldName),
                    () -> applyRename(p, dir, newName),
                    () -> {
                    }));
        });
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-037.
     * <p>
     * The undo and redo reverses pass no {@code onDone}: they are confirmed as
     * "Undone" and "Redone" by their own actions, and a second balloon saying it
     * was renamed would double-report one keystroke (#62).
     * <p>
     * Refused for the same reasons as the rename itself, before anything moves.
     * The reverse used to go straight to the rename, which renames the generated
     * code first: the class went back to the old name, the folder rename then
     * failed on the sibling holding it, and the history said "Undone" over a
     * tree and code that no longer matched (#312, A63). False tells the history
     * nothing came back, so it neither confirms nor spends the press.
     */
    private boolean applyRename(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        if (refused(p, dir, newName)) return false;

        NodeRename.apply(p, Services.getInstance(p, TreePanel.class), dir, newName, () -> {
        });
        return true;
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-004, Rule-CODEGEN-080, Rule-CODEGEN-081.
     * <p>
     * Every reason a rename is refused, asked before anything moves - by the
     * rename and by its undo and redo alike - and said when there is one.
     * <p>
     * The name is asked of the disk, not the index: only the bound project is
     * indexed, so a sibling project was invisible, and the code was renamed
     * before the folder rename failed on it.
     */
    private static boolean refused(final @NotNull Project p, final @NotNull DirectoryDto dir, final @NotNull String newName) {
        final @NotNull Notifier notifier = Services.getInstance(p, Notifier.class);

        if (Services.getInstance(p, ProjectIndexer.class).isTaken(dir.getPath().resolveSibling(newName), Optional.of(dir.getPath()))) {
            notifier.softRefuse(p, Refused.ALREADY_EXISTS, newName);
            return true;
        }

        final @NotNull Renamed renamed = new Renamed(dir, newName);
        if (renamed.packageInTheWay(p)) {
            notifier.softRefuse(p, Refused.PACKAGE_TAKEN, renamed.newPackage());
            return true;
        }

        // Code the IDE cannot look up while it indexes would stay under the old
        // name while the tree moved on, and a later rename would find nothing.
        if (DumbService.isDumb(p) && CodeOn.isOn(p) && JavaCode.of(dir.getType()).getRenamed().generates()) {
            notifier.softRefuse(p, Refused.WHILE_INDEXING, Bundle.message("dialog.rename.title"));
            return true;
        }

        return false;
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-104, Rule-TREE-PANEL-111.
     * <p>
     * Why this node cannot be renamed right now, and empty when it can. One
     * answer for the gray entry and for the keystroke, which the platform sends
     * whatever the entry looked like.
     */
    private static @NotNull Optional<String> whyNot(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        if (!dir.isRenamable()) return Optional.of(Bundle.message("rename.disabled.description"));
        if (Services.getInstance(p, TestinEditors.class).busyUnder(p, dir)) return Optional.of(Bundle.message("rename.disabled.busy"));

        return Optional.empty();
    }

    /**
     * UC-TREE-PANEL-011, Rule-TREE-PANEL-104.
     * <p>
     * Renaming is about one node, so several selected grays it rather than
     * quietly renaming the first (#192).
     * <p>
     * Also the guard that keeps a declared key to itself: the answer is empty
     * when the keystroke arrived anywhere but the Testin tree, so this is gray in
     * a Java file rather than renaming whatever the tree happens to hold behind
     * it (#119).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<String> why = Optional.ofNullable(e.getProject())
                .flatMap(p -> TestinData.singleSelectedNode(e).map(dir -> whyNot(p, dir)))
                .orElse(Optional.of(Bundle.message("rename.disabled.description")));

        GrayWithReason.unless(this, e, why.isEmpty(), why.orElse(""));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
