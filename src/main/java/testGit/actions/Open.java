package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestEditor;
import testGit.editorPanel.testRunEditor.RunEditor;
import testGit.pojo.tree.dirs.Directory;
import testGit.pojo.tree.dirs.TestRunDirectory;
import testGit.pojo.tree.dirs.TestSetDirectory;
import testGit.projectPanel.ProjectPanel;
import testGit.util.KeyboardSet;
import testGit.util.Tools;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class Open extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Open(ProjectPanel projectPanel, SimpleTree tree) {
        super("Open", "Open selected test set", AllIcons.Actions.MenuOpen);
        this.projectPanel = projectPanel;
        this.tree = tree;

        this.registerCustomShortcutSet(KeyboardSet.Enter.getShortcut(), tree);
    }

    public static void execute(ProjectPanel projectPanel, SimpleTree tree) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        if (node.getUserObject() instanceof Directory pkg) {
            if (Tools.isEditorOpen(pkg.getName())) {
                System.out.println("Editor already open, focusing: " + pkg.getName());
                return;
            }

            System.out.println("Opening Test Set: " + pkg.getPath());
            if (pkg instanceof TestSetDirectory ts)
                TestEditor.open(ts);

            if (pkg instanceof TestRunDirectory tr)
                RunEditor.open(tr, projectPanel);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        execute(projectPanel, tree);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                (node.getUserObject() instanceof TestSetDirectory ||
                        node.getUserObject() instanceof TestRunDirectory));

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}