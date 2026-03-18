package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.tree.dirs.TestSetDirectory;
import testGit.util.Notifier;
import testGit.util.Runner.TestNGRunnerByClass;
import testGit.util.Tools;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;


public class RunTestSet extends DumbAwareAction {
    private final SimpleTree tree;

    public RunTestSet(final SimpleTree tree) {
        super("Run Test Set", "Run selected test set", AllIcons.RunConfigurations.TestState.Run);
        this.tree = tree;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        if (tree == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TreePath path = tree.getSelectionPath();
        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        e.getPresentation().setEnabled(userObject instanceof TestSetDirectory);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // Double-check the path is still selected
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        // Extract the selected Directory object
        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();

        if (userObject instanceof TestSetDirectory ts) {
            // 1. Convert the physical File path into a Java FQCN
            System.out.println(this.getClass() + "directory file: " + ts.getPath().toFile());
            String fqcn = Tools.fileToFqcn(ts.getPath().toFile());
            System.out.println(this.getClass() + "fqcn path: " + fqcn);

            // 2. Trigger the high-performance background runner!
            if (fqcn != null && !fqcn.trim().isEmpty()) {
                System.out.println("fqcn: " + fqcn);
                TestNGRunnerByClass.runTestClass(fqcn);
            } else {
                Notifier.error("Run Failed", "Could not parse class name from file path: " + ts.getPath().toFile().getName());
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
