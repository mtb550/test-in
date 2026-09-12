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

package org.testin.testcase;

import org.testin.codegen.GenType;
import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.TestinEditor;
import org.testin.editor.toolbar.Toolbar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.testcase.create.TestCaseUpdateMenuDialog;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * UC-EDITOR-PANEL-006.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and makes F2 remappable in Settings -> Keymap. That is why it has no
 * constructor and no fields: the platform builds one instance for the whole IDE,
 * so the editor and the cases come from the keystroke.
 * <p>
 * F2 is in the keymap rather than on the list, because no other surface claims
 * it: the run editor's own F2 is a different action that grays itself here, and
 * the view panel's details tab binds whatever this action is bound to rather
 * than a key of its own.
 */
public class UpdateTestCaseAction extends DumbAwareAction {

    // UC-EDITOR-PANEL-006
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.editor(e).ifPresent(editor -> new Work(p, editor).overSelection(TestCaseUpdateMenuDialog::show));
    }

    /**
     * UC-EDITOR-PANEL-006.
     * <p>
     * The same update, started at one field instead of at the menu - what a
     * field's letter opens while a card is selected.
     * <p>
     * A method rather than the action object the menu used to keep: a declared
     * action is built by the platform and there is nothing for a caller to hold
     * (#119). The editor is what the letter is bound beside, and it is what
     * knows which cases are selected.
     */
    public static void openField(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull UpdateTestCaseFields field) {
        new Work(p, editor).overSelection(menu -> menu.open(field));
    }

    /**
     * UC-EDITOR-PANEL-006, Rule-EDITOR-PANEL-194.
     * <p>
     * On a selected test case in a test editor, and gray everywhere else -
     * including in a run editor, where F2 is the run item's own action and a
     * case's fields are not what a verdict is about. Asked of the node rather
     * than of the editor's type, the way Delete Test Case asks it.
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.editor(e)
                .map(TestinEditor::getParent)
                .filter(DirectoryDto::isTestCaseContainer)
                .isPresent()
                && !TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /**
     * Updating one selection, for a project and an editor that are there.
     */
    private record Work(@NotNull Project p, @NotNull TestinEditor editor) {

        /**
         * The selected cases, and what follows an accepted update over them.
         * <p>
         * Both ways in need every line of it - the indexer, the balloon, the
         * toolbar's filter, the repaint - and neither has a reason to differ, so
         * the aftermath is written where they meet rather than in each of them.
         */
        private void overSelection(final @NotNull Consumer<TestCaseUpdateMenuDialog> open) {
            final @NotNull List<TestCaseDto> selectedItems = editor.getSelectedTestCases();
            if (selectedItems.isEmpty()) return;

            final @NotNull Path path = editor.getParent().getPath();

            Logger.trace("update test cases: " + selectedItems.stream().map(TestCaseDto::getDescription).collect(Collectors.joining(", ")));

            // Taken before the menu opens, not inside its callback: the menu edits
            // the very DTOs it was handed, so by the time it says what changed, the
            // values it changed them from are already gone.
            final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(selectedItems);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, path, ids);

            open.accept(new TestCaseUpdateMenuDialog(p, selectedItems, (updatedItems, gt) -> {

                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                boolean changed = false;
                for (final TestCaseDto tc : updatedItems)
                    changed |= indexer.putTestCase(path, tc);

                // A save that changed nothing is not an update. Nothing was stamped
                // and nothing was written, so there is nothing to confirm, nothing
                // to take back and no method to regenerate - and saying "Updated"
                // for it is how a tester came to be recorded as having edited a case
                // they only looked at (#164).
                if (!changed) return;

                // One operation for the whole selection, recorded outside the loop
                // above. Inside it, a bulk edit over forty cases would cost forty
                // presses of CTRL+Z to take back (#165).
                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.update"), updatedItems), before, TestCaseSnapshot.of(p, path, ids));

                // Reordering says Re-sorted whichever way it was done. Dragging a
                // card already said it and typing a position said Updated, so the
                // same act had two words depending on the gesture (#210).
                Services.getInstance(p, Notifier.class).softShow(p,
                        gt == GenType.UPDATE_TEST_CASE_ORDER ? Done.RE_SORTED : Done.UPDATED);

                if (editor instanceof Toolbar)
                    ((Toolbar) editor).onToolBarFilterSelectionChanged();

                ApplicationManager.getApplication().invokeLater(() -> {
                    // Ordered rather than repainted: the Order field writes a rank,
                    // which moves the case and renumbers every card after it.
                    editor.refreshOrdered();
                    TestCaseUpdateMenuDialog.applyAftermath(p, updatedItems, gt);
                });
            }));
        }
    }
}
