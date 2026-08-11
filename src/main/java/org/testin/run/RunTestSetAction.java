package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.runner.TestNGRunnerByClass;
import org.testin.services.Services;
import org.testin.util.OptionalPlugin;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;

public class RunTestSetAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public RunTestSetAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Run Test Set", "Run selected test set", AllIcons.RunConfigurations.TestState.Run);
        this.p = p;
        this.tree = tree;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());
        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        Object userObject = TreeValueUtil.valueOf(path.getLastPathComponent());

        if (userObject instanceof TestSetDirectoryDto ts) {
            if (!OptionalPlugin.TESTNG.isAvailableOrWarn(p)) return;

            Logger.info(this.getClass() + "directory file: " + ts.getPath().toFile());
            // Build the FQCN the same way the code generator does: strip the
            // "Test Cases" display node and sanitize each segment. The raw path2
            // join produced a class name findClass could never resolve.
            String fqcn = String.join(".", Services.getInstance(p, Tools.class).buildFqcnClass(p, ts));
            Logger.info(this.getClass() + "fqcn path: " + fqcn);

            if (!fqcn.trim().isEmpty()) {
                Logger.info("fqcn: " + fqcn);
                Services.getInstance(p, TestNGRunnerByClass.class).runTestClass(p, fqcn);

            } else
                Services.getInstance(p, Notifier.class).error(p, "Run Failed", "Could not parse class name from file path: " + ts.getPath().toFile().getName());

        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
