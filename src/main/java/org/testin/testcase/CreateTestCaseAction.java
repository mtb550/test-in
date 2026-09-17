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

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.codegen.GenType;
import org.testin.editor.TestinEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-EDITOR-PANEL-005.
 * <p>
 * Declared in {@code plugin.xml} (#119) with Ctrl+M, the same key the tree's
 * Create carries - and now both are in the keymap, so rebinding Create rebinds
 * it in both places. Until this one was declared the tree's key was rebindable
 * and the editor's was not, which is a split a tester would meet and nothing
 * would explain.
 */
public class CreateTestCaseAction extends DumbAwareAction {

    // UC-EDITOR-PANEL-005
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        // The same question update() asks, so the two cannot drift: an entry
        // that is black and does nothing is the other half of one that is gray
        // and says nothing (#312, A87). An editor's parent is a test set or a
        // test run, and only the set answers true, which is what makes the cast
        // safe here and nowhere else.
        TestinData.editor(e)
                .filter(editor -> editor.getParent().isTestCaseContainer())
                .ifPresent(editor -> openCreateDialog(p, editor, (TestSetDirectoryDto) editor.getParent()));
    }

    // UC-EDITOR-PANEL-005, Rule-EDITOR-PANEL-008, Rule-EDITOR-PANEL-030
    public static void openCreateDialog(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull TestSetDirectoryDto dir) {
        new CreateTestCaseDialog(p, dir, tc -> {
            // No rank here. The case arrives unranked, which sorts it last -
            // and the append path already ranks the list it just sorted, so it
            // sees this case at the end and gives it a rank after everything.
            //
            // Computing it here got that wrong in exactly the sets that need it
            // most. It read the rank off the last case on screen, and an
            // unranked case sorts last: a set that had picked one up - imported,
            // copied in by hand, or brought by a merge - answered with no rank
            // at all, and "after nothing" resolves to the middle. The new case
            // appeared in the middle of the set until the reorder that follows
            // repaired it, writing a second version of a file written a moment
            // earlier.
            tc.setParent(dir);

            final @NotNull List<TestCaseDto> affectedNodes = List.of(tc);

            // Before the case exists anywhere: the index has never heard of this
            // id, which is what the snapshot records and what undoing a creation
            // puts back.
            final @NotNull List<UUID> ids = TestCaseSnapshot.idsOf(affectedNodes);
            final @NotNull TestCaseSnapshot before = TestCaseSnapshot.of(p, dir.getPath(), ids);

            // Written once, by the sequence write the append starts: it sees this
            // case for the first time, stamps it as created, gives it its rank and
            // writes it. A direct save here as well wrote the file a second time -
            // first unranked, then ranked (#66, finding 115).
            //
            // So everything that needs the case to exist runs from the callback,
            // after that write: the undo record, which reads what a redo would
            // write, and - back on the UI thread - the balloon saying the case
            // exists and the code generation, which both used to run against a
            // case the indexer had not been told about yet.
            editor.appendNewTestCase(tc, () -> {
                TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.create"), affectedNodes), before, TestCaseSnapshot.of(p, dir.getPath(), ids));

                ApplicationManager.getApplication().invokeLater(() -> {
                    Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);
                    GenType.CREATE_TEST_CASE.getAction().execute(p, tc);
                });
            });
            Services.getInstance(p, TestCaseValues.class).addNewItems(affectedNodes);
        }).show();
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-214
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Asked of the node rather than of the editor's class, which is what
        // Delete next to it already does: "can this hold test cases" is the
        // node's question, and asking the class means a node kind added later
        // answers wrongly until somebody remembers this method (#312, A87).
        final @NotNull Optional<TestinEditor> editor = TestinData.editor(e);
        final boolean holdsCases = editor.filter(open -> open.getParent().isTestCaseContainer()).isPresent();

        // And not while the set is still being read. Creating a case sorts the
        // list, and a sort makes the load still in flight stale - so the load
        // landed on nothing, the spinner never stopped, and the new case was
        // ranked against a list nobody had seen, which put it in the middle of
        // the set at the next Refresh (#312, A18).
        final boolean loading = editor.filter(TestinEditor::isLoading).isPresent();
        final boolean enabled = holdsCases && !loading;

        e.getPresentation().setEnabled(enabled);

        // And a reason where there was none. A tester in a run editor pressed
        // Ctrl+M and got nothing - correct, and indistinguishable from a key
        // that is not bound.
        if (!enabled && editor.isPresent()) {
            e.getPresentation().setDescription(loading
                    ? Bundle.message("create.case.still.loading")
                    : Bundle.message("create.case.disabled.description"));
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT now: update() reads which editor has the keyboard, which the
        // platform answers from Swing state (#52, #119).
        return ActionUpdateThread.EDT;
    }
}
