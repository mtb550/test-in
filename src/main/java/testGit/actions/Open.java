package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestCaseEditor;
import testGit.editorPanel.testRunEditor.TestRunEditor;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;
import testGit.projectPanel.ProjectPanel;
import testGit.util.KeyboardSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class Open extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Open(ProjectPanel projectPanel, SimpleTree tree) {
        super("Open", "Open selected test set", AllIcons.Actions.MenuOpen);
        this.projectPanel = projectPanel;
        this.tree = tree;

        this.registerCustomShortcutSet(KeyboardSet.Enter.get(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        if (node.getUserObject() instanceof TestPackage pkg) {
            System.out.println("Opening Test Set: " + pkg.getFilePath());
            if (pkg.getType() == DirectoryType.TS)
                TestCaseEditor.open(pkg);

            if (pkg.getType() == DirectoryType.TR)
                TestRunEditor.open(pkg, projectPanel);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof TestPackage pkg &&
                (pkg.getType() == DirectoryType.TS ||
                        pkg.getType() == DirectoryType.TR)
        );

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}