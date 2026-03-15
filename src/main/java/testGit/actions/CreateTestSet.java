package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.testCaseEditor.TestCaseEditor;
import testGit.pojo.DirectoryIcon;
import testGit.pojo.DirectoryType;
import testGit.pojo.TestPackage;
import testGit.util.TreeUtilImpl;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class CreateTestSet extends DumbAwareAction {
    private final SimpleTree tree;

    public CreateTestSet(SimpleTree tree) {
        super("New Test Set", "Create a new test set", AllIcons.Nodes.Class);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@Nullable AnActionEvent e) {
        System.out.println("AddTestSet.actionPerformed()");
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof TestPackage treeItem) || treeItem.getType() == DirectoryType.TS || treeItem.getType() == DirectoryType.TR)
            return;

        String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
        if (name == null || name.isBlank()) return;
        name = name.replace("_", " ");

        TestPackage newTestSet = new TestPackage()
                .setType(DirectoryType.TS)
                .setName(name)
                .setIcon(DirectoryIcon.TS);

        newTestSet.setFileName(String.format("%s_%s", newTestSet.getType().toString(), newTestSet.getName()));

        newTestSet.setFilePath(treeItem.getFilePath().resolve(newTestSet.getFileName()));

        System.out.println("AddTestSet.actionPerformed(): newTestSet = " + newTestSet.getFilePath());
        newTestSet.setFile(newTestSet.getFilePath().toFile());

        TreeUtilImpl.insertVf(this, treeItem.getFilePath(), newTestSet.getFileName());

        TreeUtilImpl.insertNode(tree, parentNode, newTestSet);
        TestCaseEditor.open(newTestSet);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof TestPackage item &&
                item.getType() != DirectoryType.TS &&
                item.getType() != DirectoryType.TR &&
                !item.getFilePath().toString().contains("TRP_testRuns")
        );

        assert path != null;
        if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof TestPackage item)
            System.out.println("item.getFilePath(): " + item.getFilePath().toString());

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}