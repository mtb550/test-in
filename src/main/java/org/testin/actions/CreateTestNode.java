package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.CreateNodeMenu;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.KeyboardSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTestNode extends DumbAwareAction {
    @Getter
    private final ProjectPanel projectPanel;
    @Getter
    private final SimpleTree tree;

    public CreateTestNode(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(parentNode.getUserObject() instanceof DirectoryDto parentDir)) return;

        CreateNodeMenu menu = parentDir.getMenu();

        // todo, activate checkbox -> setting button.
        new CreateNodesDialog(menu, null, (enteredName, selectedType) -> {
            if (enteredName == null || enteredName.isEmpty()) return;

            Path newDirPath = parentDir.getPath().resolve(enteredName);

            if (selectedType != null && selectedType.getCreator() != null)
                selectedType.getCreator().execute(this, e.getProject(), enteredName, parentNode, parentDir, newDirPath);
            else
                System.out.println("No creation logic defined for type: " + selectedType);

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(parentNode.getUserObject() instanceof DirectoryDto parentDir)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(!parentDir.getMenu().getAvailableOptions().isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}