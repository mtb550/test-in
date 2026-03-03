package testGit.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.testCaseEditor.TestCaseEditor;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;
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
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("AddTestSet.actionPerformed()");
        if (tree == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = parentNode.getUserObject();

        if (!(userObject instanceof Directory treeItem) || treeItem.getType() == DirectoryType.TS) return;

        String name = Messages.showInputDialog("Enter feature name:", "Add Feature", null);
        if (name == null || name.isBlank()) return;
        name = name.replace("_", " ");

        Directory newTestSet = new Directory()
                .setType(DirectoryType.TS)
                .setName(name)
                .setActive(1);

        newTestSet.setFileName(String.format("%s_%s_%d", newTestSet.getType().toString().toLowerCase(), newTestSet.getName(), newTestSet.getActive()));

        if (treeItem.getType() == DirectoryType.PR)
            newTestSet.setFilePath(treeItem.getFilePath().resolve("testCases").resolve(newTestSet.getFileName()));
        else
            newTestSet.setFilePath(treeItem.getFilePath().resolve(newTestSet.getFileName()));


        System.out.println("AddTestSet.actionPerformed(): newTestSet = " + newTestSet.getFilePath());
        newTestSet.setFile(newTestSet.getFilePath().toFile());

        TreeUtilImpl.insertVf(this, newTestSet.getFilePath(), newTestSet.getFileName());

        DefaultMutableTreeNode newNode = TreeUtilImpl.insertNode(tree, parentNode, newTestSet);
        TestCaseEditor.open(newTestSet);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean isFeature = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof Directory item &&
                item.getType() == DirectoryType.TS);

        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(!isFeature);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}