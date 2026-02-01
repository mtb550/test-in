package testGit.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.TestCaseEditor;
import testGit.pojo.Directory;
import testGit.util.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;

public class OpenFeatureActionContext extends AnAction {
    private final SimpleTree tree;

    public OpenFeatureActionContext(final SimpleTree tree) {
        super("▢ Open");
        this.tree = tree;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        // منطق فتح الـ Feature (ملفات JSON)
        if (node.getUserObject() instanceof Directory dir && dir.getType() == NodeType.FEATURE.getCode()) {
            TestCaseEditor.open(dir.getFilePath());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // التحكم في الظهور في القوائم
        boolean isFeature = false;
        if (tree != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            isFeature = (node != null && node.getUserObject() instanceof Directory dir
                    && dir.getType() == NodeType.FEATURE.getCode());
        }
        //e.getPresentation().setEnabledAndVisible(isFeature);
        e.getPresentation().setEnabled(isFeature);
    }

}