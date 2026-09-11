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
import org.testin.editor.test.TestEditor;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.create.CreateTestCaseDialog;
import org.testin.util.Bundle;

import java.util.List;
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

        TestinData.editor(e)
                .filter(TestEditor.class::isInstance)
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

            // Recorded from the callback, because the rank arrives after the
            // save: the case is written here and placed by the sort that
            // follows, so what a redo would have to write is not readable yet.
            editor.appendNewTestCase(tc, () -> TestCaseSnapshot.record(p, TestCaseSnapshot.describe(Bundle.message("snapshot.verb.create"), affectedNodes), before, TestCaseSnapshot.of(p, dir.getPath(), ids)));
            Services.getInstance(p, TestCaseCacheService.class).addNewItems(affectedNodes);

            // Directly, as the other three savers do. This went through a
            // service that deferred the write behind an invokeLater and a write
            // action, so everything below it - the code generation and the
            // balloon saying the case exists - ran against a case the indexer
            // had not been told about yet.
            Services.getInstance(p, ProjectIndexer.class).putTestCase(dir.getPath(), tc);
            Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);

            GenType.CREATE_TEST_CASE.getAction().execute(p, tc);

            ApplicationManager.getApplication().invokeLater(() -> editor.selectTestCase(tc));

        }).show();
    }

    // UC-EDITOR-PANEL-005, UC-EDITOR-PANEL-030
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.editor(e).filter(TestEditor.class::isInstance).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT now: update() reads which editor has the keyboard, which the
        // platform answers from Swing state (#52, #119).
        return ActionUpdateThread.EDT;
    }
}
