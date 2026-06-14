package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.pojo.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class Rename extends DumbAwareAction {
    private final ProjectPanel projectPanel;
    private final SimpleTree tree;

    public Rename(final ProjectPanel projectPanel, final SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.RenameNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (tree == null || e.getProject() == null) return;

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof DirectoryDto dir)) return;
        if (dir instanceof TestCasesMainDirectoryDto || dir instanceof TestRunsMainDirectoryDto) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Services.getInstance(e.getProject(), EditorUtil.class).closeEditor(e.getProject(), dir.getName());

        Path oldPath = dir.getPath();
        Path newPath = oldPath.getParent().resolve(newName);

        Services.getInstance(e.getProject(), TreeUtilImpl.class).executeVfsAction(e.getProject(), oldPath, "Rename Failed", vf -> {
            vf.rename(this, newName);

            dir.setName(newName);
            dir.setPath(newPath);
            dir.setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            dir.setModifiedBy("Muteb almughyiri");

            Services.getInstance(e.getProject(), Tools.class).updateChildrenPathsRecursive(node, oldPath, newPath);
            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

            if (dir instanceof TestProjectDirectoryDto && projectPanel.getTestProjectSelector() != null) {
                projectPanel.getTestProjectSelector().loadTestProjectList();
            }

            Log.info("Success! Renamed to: " + newName);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        boolean shouldEnable = (path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof DirectoryDto dir &&
                !(dir instanceof TestCasesMainDirectoryDto) &&
                !(dir instanceof TestRunsMainDirectoryDto)
        );

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}