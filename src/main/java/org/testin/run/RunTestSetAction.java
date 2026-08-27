package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;

/**
 * Runs every test case under the selected node, as one run.
 * <p>
 * Offered on anything that holds test cases - a test set, a package of them, the
 * Test Cases root - rather than on a test set alone. The node itself answers
 * whether it holds any, so this asks {@code isTestCaseContainer()} instead of
 * naming the three kinds that do, and a fourth kind would be offered it without
 * anything here changing.
 */
public class RunTestSetAction extends AbstractProjectTreeAction {

    public RunTestSetAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Run Tests", "Run every test case in the selected test set or package",
                AllIcons.RunConfigurations.TestState.Run);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(runnable().isPresent());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        runnable().ifPresent(this::run);
    }

    /**
     * The selected node, when it is one that holds test cases.
     */
    private @NotNull Optional<DirectoryDto> runnable() {
        return TreeValueUtil.selected(tree, DirectoryDto.class).filter(DirectoryDto::isTestCaseContainer);
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
            Services.getInstance(p, Notifier.class).softShow(p, dir.getName() + " has no test cases to run");
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
