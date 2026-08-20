package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.codegen.Fqcn;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.runner.TestNGRunnerByClass;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;


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

    private void runTestSet(final @NotNull TestSetDirectoryDto ts) {
        if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

        Logger.info(this.getClass() + "directory file: " + ts.getPath().toFile());
        // Build the FQCN the same way the code generator does: strip the
        // "Test Cases" display node and sanitize each segment. The raw path2
        // join produced a class name findClass could never resolve.
        final String fqcn = String.join(".", Fqcn.ofClass(p, ts));
        Logger.info(this.getClass() + "fqcn path: " + fqcn);

        if (fqcn.trim().isEmpty()) {
            Services.getInstance(p, Notifier.class).error(p, "Run Failed", "Could not parse class name from file path: " + ts.getPath().toFile().getName());
            return;
        }

        Logger.info("fqcn: " + fqcn);
        Services.getInstance(p, TestNGRunnerByClass.class).runTestClass(p, fqcn);

        // The same word Run Test Case uses, for the same reason: the run
        // starts elsewhere and the tree gives no sign it was heard (#62).
        Services.getInstance(p, Notifier.class).softShow(p, "Running");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
