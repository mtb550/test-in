package org.testin.testrun;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.editor.run.RunEditor;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.testrun.create.FailedResultDialog;
import org.testin.view.ViewToolWindowFactory;

import java.util.List;
import java.util.Optional;

/**
 * UC-EDITOR-PANEL-040.
 * <p>
 * Declared in {@code plugin.xml} (#119), which is what puts it in Find Action
 * and makes F2 remappable in Settings -> Keymap. No constructor and no fields:
 * the platform builds one instance for the whole IDE, so the run editor and the
 * case come from the keystroke.
 * <p>
 * F2 as well as Update Test Case, and the two never compete: each grays itself
 * where the other belongs, so only one of them is ever enabled. That is what
 * lets one key mean "edit what is in front of me" in both editors.
 */
public class UpdateRunItemAction extends DumbAwareAction {

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        // Nothing selected is nothing to edit, and neither is anywhere but a run.
        final @NotNull Optional<TestCaseDto> selected = TestinData.singleSelectedCase(e);
        final @NotNull Optional<RunEditor> runEditor = runEditor(e);
        if (selected.isEmpty() || runEditor.isEmpty()) return;

        edit(p, runEditor.orElseThrow(), selected.orElseThrow());
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-167
    private void edit(final @NotNull Project p, final @NotNull RunEditor runEditor, final @NotNull TestCaseDto testCase) {
        final @NotNull Optional<TestRunItems> found = runEditor.runItem(testCase.getId());
        if (found.isEmpty()) return;

        final @NotNull TestRunItems runItem = found.orElseThrow();

        // The test case is gone: what the run recorded against it stands as it is.
        if (runItem.isRemoved()) {
            Services.getInstance(p, RunStatusService.class).refuseRemoved(p);
            return;
        }

        Logger.trace("update test run item for: " + testCase.getDescription());

        // The same details dialog that opens automatically on a Failed status;
        // F2 edits without touching the status.
        new FailedResultDialog(p, runItem, () -> runEditor.run().ifPresentOrElse(tr -> {
            Services.getInstance(p, ProjectIndexer.class).persistRun(runEditor.getParent().getPath(), tr);

            // After the dialog has gone, not inside its submit. FailedResultDialog
            // runs this callback before it closes, so a rebuild started here would
            // ask the grid whether it has the keyboard while the modal dialog
            // still owns it - answer no - and hand the focus nowhere, which is the
            // defect the verdict path already had fixed.
            ApplicationManager.getApplication().invokeLater(() -> {
                // Whatever the tester is looking at, not the list alone. This
                // repainted the JList, and the grid is a separate table built from
                // a snapshot - so a stacktrace or an actual result typed here was
                // on disk and correct while the grid went on showing the old value
                // until something else happened to rebuild it. The same call the
                // test-case update already makes.
                runEditor.refreshView();

                // And the View panel, which holds its own copy of the case - so
                // the details a tester was reading kept the value they had just
                // changed.
                ViewToolWindowFactory.refreshIfShowing(p, List.of(testCase));
            });

            // After the persist: an edit that was dropped rather than saved must
            // not report itself as saved (#62).
            Services.getInstance(p, Notifier.class).softShow(p, "Details updated");

            // The editor empties the run while it reloads. Persisting is the whole
            // point of the callback, so say the edit was dropped rather than lose it
            // quietly.
        }, () -> Logger.warn("Run item edited while the run was reloading; not persisted"))).show();
    }

    // UC-EDITOR-PANEL-040, Rule-EDITOR-PANEL-168
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Details belong to failed test cases only - the dialog's title stays
        // truthful and the action reads as what it is. And to a run editor only,
        // which is what keeps F2 to one meaning at a time (#119).
        e.getPresentation().setEnabled(runEditor(e)
                .flatMap(runEditor -> TestinData.singleSelectedCase(e).flatMap(tc -> runEditor.runItem(tc.getId())))
                .filter(item -> item.getStatus() == TestStatus.FAILED)
                .isPresent());
    }

    /**
     * The run editor the keystroke arrived in, and empty anywhere else - a test
     * set editor included, which answers the same data key and has no run.
     */
    private @NotNull Optional<RunEditor> runEditor(final @NotNull AnActionEvent e) {
        return TestinData.editor(e).filter(RunEditor.class::isInstance).map(RunEditor.class::cast);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() reads Swing selection and editor state - EDT only.
        return ActionUpdateThread.EDT;
    }
}
