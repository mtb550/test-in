package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.editor.TestinEditor;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.EditorUtil;

import java.util.List;
import java.util.Optional;

/**
 * Runs what the selected node holds, as one run.
 * <p>
 * Two nodes offer it and they are not the same gesture underneath.
 * <p>
 * A node that <b>holds test cases</b> - a test set, a package of them, the Test
 * Cases root - hands its cases straight to the runner. Nothing records a verdict,
 * because there is no run for one to belong to; the cases get a badge and the
 * generated methods do what they do.
 * <p>
 * A <b>test run</b> goes through its editor instead. That is not a detour: a
 * verdict reaches a named run only through the editor that claimed the case, so a
 * launch from here that skipped the editor would execute everything and write
 * nothing into the run the tester right-clicked. The editor is opened or focused
 * and told to start once it has read its cases.
 * <p>
 * The node is asked which it is rather than told: {@code isTestCaseContainer()}
 * answers the first question and a typed selection answers the second, which is
 * the idiom every other run-only tree action already uses. It is deliberately
 * <b>not</b> extended by making a run a test case container - that flag means
 * "test cases can be imported into or exported from this node", and Import and
 * Export read it too.
 */
public class RunTestsAction extends AbstractProjectTreeAction {

    public RunTestsAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Run Tests", "Run every test case the selected node holds", AllIcons.RunConfigurations.TestState.Run);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(runnable().isPresent() || selectedRun().isPresent());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        selectedRun().ifPresentOrElse(this::openAndRun, () -> runnable().ifPresent(this::run));
    }

    /**
     * The selected node, when it is one that holds test cases.
     */
    private @NotNull Optional<DirectoryDto> runnable() {
        return TreeValueUtil.selected(tree, DirectoryDto.class).filter(DirectoryDto::isTestCaseContainer);
    }

    /**
     * The selected test run, and empty once it has been signed off.
     * <p>
     * Greyed rather than offered and refused, which is what the menu does
     * everywhere else and what {@code SetTestRunStatusAction} does on this same
     * node. A completed or closed run records no more verdicts, so running it
     * would burn a compile and a JVM to throw the results away.
     */
    private @NotNull Optional<TestRunDirectoryDto> selectedRun() {
        return TreeValueUtil.selected(tree, TestRunDirectoryDto.class)
                .filter(run -> !run.getMarker().getStatus().isTerminal());
    }

    /**
     * Hands the run to its own editor, which owns starting it.
     * <p>
     * Through the editor rather than straight to the runner, because a verdict
     * reaches a named run only through the editor that claimed the case - see
     * {@code RunEditor.launching}. A launch nobody claims leaves a badge that
     * dies with the IDE and writes nothing into any run, which is the whole of
     * what the tester asked for.
     * <p>
     * The editor may not exist yet and, once it does, may not have read its
     * cases yet. {@link EditorUtil#openThen} owns the first wait and
     * {@link TestinEditor#runWhenLoaded()} owns the second, so this says only
     * what it wants: open that run, and start it.
     * <p>
     * Which of its cases run, and the refusal when none of them do, are the
     * editor's answer and the same one the Start button beside it gives - so the
     * gesture means the same thing wherever it is reached from.
     */
    private void openAndRun(final @NotNull TestRunDirectoryDto run) {
        Services.getInstance(p, EditorUtil.class).openThen(p, run, TestinEditor::runWhenLoaded);
    }

    /**
     * Runs what is under the node, as one run.
     * <p>
     * The cases are asked for by name rather than the class they generate into.
     * Handing the runner a class name was what left this unable to mark a card,
     * refuse a case with no generated method, or be stopped at all - none of
     * which is about running one process, and all of which is about not knowing
     * what is in it (#36).
     */
    private void run(final @NotNull DirectoryDto dir) {
        final @NotNull List<TestCaseDto> cases = Services.getInstance(p, ProjectIndexer.class).getTestCasesUnder(dir);

        if (cases.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuseNothingToRun(p, dir.getName());
            return;
        }

        Logger.info("Running " + dir.getName() + " with " + cases.size() + " test case(s)");

        // Through the class that owns starting a run, not straight to the
        // runner. It drops the cases already going and counts what actually
        // started; this went round it, so starting a case from the editor and
        // then running its set launched that case a second time while the first
        // was still running, and the balloon counted every case under the node
        // whether it started or not.
        //
        // The TestNG guard and the notification went with it - both are that
        // class's, and both were written out again here.
        RunTestCases.run(p, cases);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
