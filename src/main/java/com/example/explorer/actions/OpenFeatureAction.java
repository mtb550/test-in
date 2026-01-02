package com.example.explorer.actions;

import com.example.editor.TestCaseEditor;
import com.example.pojo.Directory;
import com.example.util.NodeType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class OpenFeatureAction extends AbstractAction {
    private final JTree tree;
    private final KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    public OpenFeatureAction(JTree tree) {
        this.tree = tree;
    }

    /**
     * تسجيل الأكشن ليعمل مع مفتاح Enter
     */
    public static void register(JTree tree) {
        OpenFeatureAction action = new OpenFeatureAction(tree);
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(action.key, "openFeature");
        tree.getActionMap().put("openFeature", action);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;

        // منطق فتح الـ Feature (ملفات JSON)
        if (node.getUserObject() instanceof Directory dir && dir.getType() == NodeType.FEATURE.getCode()) {
            TestCaseEditor.open(dir.getFilePath());
        }
    }
}