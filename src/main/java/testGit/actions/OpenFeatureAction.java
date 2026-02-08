package testGit.actions;

import com.intellij.ui.treeStructure.SimpleTree;
import testGit.editorPanel.TestCaseEditor;
import testGit.pojo.Directory;
import testGit.pojo.DirectoryType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class OpenFeatureAction extends AbstractAction {
    private final SimpleTree tree;
    private final KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    public OpenFeatureAction(SimpleTree tree) {
        this.tree = tree;
    }

    /**
     * تسجيل الأكشن ليعمل مع مفتاح Enter
     */
    public static void register(SimpleTree tree) {
        OpenFeatureAction action = new OpenFeatureAction(tree);
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(action.key, "openFeature");
        tree.getActionMap().put("openFeature", action);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        // منطق فتح الـ Feature (ملفات JSON)
        if (node.getUserObject() instanceof Directory dir && dir.getType() == DirectoryType.F) {
            TestCaseEditor.open(dir.getFilePath());
        }
    }
}