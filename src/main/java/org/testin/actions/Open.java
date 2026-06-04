package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestRunDirectoryDto;
import org.testin.pojo.dto.dirs.TestSetDirectoryDto;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.logger.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class Open extends DumbAwareAction {
    private final SimpleTree tree;

    public Open(final SimpleTree tree) {
        super("Open", "Open selected test sets or runs", AllIcons.Actions.MenuOpen);
        this.tree = tree;

        this.registerCustomShortcutSet(KeyboardSet.Enter.getCustomShortcut(), tree);
    }

    public void execute(final @NotNull Project project) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return;

        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (node == null) continue;
            if (!(node.getUserObject() instanceof DirectoryDto directoryDto)) return;


            if (directoryDto instanceof TestSetDirectoryDto ts) {
                Log.info("open test set: " + ts.getPath());
                EditorUtil.getInstance().openEditorIfNotOpen(project, ts);
                continue;
            }

            if (directoryDto instanceof TestRunDirectoryDto tr) {
                Log.info("open test run: " + tr.getPath());
                EditorUtil.getInstance().openEditorIfNotOpen(project, tr);
            }

        }
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute(e.getProject());
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean shouldEnable = false;

        if (paths != null) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof DefaultMutableTreeNode node) {
                    Object userObject = node.getUserObject();
                    if (userObject instanceof TestSetDirectoryDto || userObject instanceof TestRunDirectoryDto) {
                        shouldEnable = true;
                        break;
                    }
                }
            }
        }

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}