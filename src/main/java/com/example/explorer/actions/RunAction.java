package com.example.explorer.actions;

import com.example.pojo.Directory;
import com.example.util.NodeType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import static com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT;

public class RunAction extends AnAction {
    public RunAction() {
        super("▶ Run Feature");
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        JTree tree = e.getData(CONTEXT_COMPONENT) instanceof JTree jTree ? jTree : null;
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
        e.getPresentation().setEnabled(userObject instanceof Directory treeItem && treeItem.getType() == NodeType.FEATURE.getCode());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // TODO: Run the feature test automation
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
