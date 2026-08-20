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
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGRunner;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;

import java.util.List;


public class RunTestSetAction extends AbstractProjectTreeAction {

    public RunTestSetAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Run Test Set", "Run selected test set", AllIcons.RunConfigurations.TestState.Run);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.selected(tree, TestSetDirectoryDto.class).isPresent());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreeValueUtil.selected(tree, TestSetDirectoryDto.class).ifPresent(this::runTestSet);
    }

    /**
     * Runs the set's cases, as one run.
     * <p>
     * The cases are asked for by name rather than the class they generate into.
     * Handing the runner a class name was what left a test set run unable to
     * mark a card, refuse a case with no generated method, or be stopped at all
     * - none of which is about running one process, and all of which is about
     * not knowing what is in it (#36).
     */
    private void runTestSet(final @NotNull TestSetDirectoryDto ts) {
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        final List<TestCaseDto> cases = Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(ts.getPath());

        if (cases.isEmpty()) {
            Services.getInstance(p, Notifier.class).softShow(p, ts.getName() + " has no test cases");
            return;
        }

        Logger.info("Running test set " + ts.getName() + " with " + cases.size() + " test case(s)");
        Services.getInstance(p, TestNGRunner.class).run(p, cases);

        // The same word Run Test Case uses, for the same reason: the run starts
        // elsewhere and the tree gives no sign it was heard (#62).
        Services.getInstance(p, Notifier.class).softShowCounted(p, "Running", cases.size());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
