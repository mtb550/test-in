package org.testin.run;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.util.logger.Logger;
import org.testin.util.notifications.Notifier;
import org.testin.util.runner.TestNGRunnerByClass;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class RunTestSet extends DumbAwareAction {
    private final @NotNull SimpleTree tree;

    public RunTestSet(final @NotNull SimpleTree tree) {
        super("Run Test Set", "Run selected test set", AllIcons.RunConfigurations.TestState.Run);
        this.tree = tree;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        e.getPresentation().setEnabled(userObject instanceof TestSetDirectoryDto);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (e.getProject() == null || path == null) return;

        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();

        if (userObject instanceof TestSetDirectoryDto ts) {
            Logger.info(this.getClass() + "directory file: " + ts.getPath().toFile());
            String fqcn = String.join(".", ts.getPath2());
            Logger.info(this.getClass() + "fqcn path: " + fqcn);

            if (!fqcn.trim().isEmpty()) {
                Logger.info("fqcn: " + fqcn);
                Services.getInstance(e.getProject(), TestNGRunnerByClass.class).runTestClass(e.getProject(), fqcn);
            } else {
                Services.getInstance(e.getProject(), Notifier.class).error(e.getProject(), "Run Failed", "Could not parse class name from file path: " + ts.getPath().toFile().getName());
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
