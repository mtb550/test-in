package org.testin.testcase;

import org.testin.notifications.Done;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
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
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.UUID;

public class CreateTestCaseAction extends AbstractProjectAction {
    private final @NotNull TestinEditor editor;
    private final @NotNull TestSetDirectoryDto dir;

    public CreateTestCaseAction(final @NotNull Project p, final @NotNull TestinEditor editor, final @NotNull TestSetDirectoryDto dir, final @NotNull JBList<TestCaseDto> list) {
        super(p, GenType.CREATE_TEST_CASE.getDescription(), "Create new test case", AllIcons.Actions.AddToDictionary);
        this.editor = editor;
        this.dir = dir;
        this.registerCustomShortcutSet(Shortcuts.CreateItem.getCustomShortcut(), list);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        openCreateDialog();
    }

    public void openCreateDialog() {
        new CreateTestCaseDialog(p, tc -> {
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
            editor.appendNewTestCase(tc, () -> TestCaseSnapshot.record(p, TestCaseSnapshot.describe("Create", affectedNodes), before, TestCaseSnapshot.of(p, dir.getPath(), ids)));
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

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(editor instanceof TestEditor);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
