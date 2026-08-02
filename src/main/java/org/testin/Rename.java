package org.testin;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestCasesMainDirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestRunsMainDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.TreeUtilImpl;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class Rename extends DumbAwareAction {
    private final @NotNull ProjectPanel projectPanel;
    private final @NotNull SimpleTree tree;

    public Rename(final @NotNull ProjectPanel projectPanel, final @NotNull SimpleTree tree) {
        super("Rename", "Rename selected node", AllIcons.Actions.Edit);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.RenameNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();

        final TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(node.getUserObject() instanceof DirectoryDto dir)) return;
        if (dir instanceof TestCasesMainDirectoryDto || dir instanceof TestRunsMainDirectoryDto) return;

        String newName = Messages.showInputDialog("Enter new name:", "Rename", AllIcons.Actions.Edit, dir.getName(), null);
        if (newName == null || newName.isBlank() || newName.equals(dir.getName())) return;

        Services.getInstance(project, EditorUtil.class).close(project, dir.getName());

        Path oldPath = dir.getPath();
        Path newPath = oldPath.getParent().resolve(newName);

        Services.getInstance(project, TreeUtilImpl.class).executeVfsAction(project, oldPath, "Rename Failed", vf -> {
            try {
                vf.rename(this, newName);
            } catch (final IOException ex) {
                Logger.error(ex.getMessage());
                throw new RuntimeException(ex);
            }

            dir.setName(newName);
            dir.setPath(newPath);
            dir.setModifiedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            dir.setModifiedBy(System.getProperty("user.name", ""));

            Services.getInstance(project, Tools.class).updateChildrenPathsRecursive(node, oldPath, newPath);
            ((DefaultTreeModel) tree.getModel()).nodeChanged(node);

            Services.getInstance(project, ProjectIndexer.class).renameNode(oldPath, newPath);

            if (dir instanceof TestProjectDirectoryDto) {
                projectPanel.getTestProjectSelector().loadTestProjectList();
            }

            Logger.info("Success! Renamed to: " + newName);

            // todo: add code generator code to change the name in automation code.

        });
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath path = tree.getSelectionPath();

        e.getPresentation().setEnabled(path != null &&
                path.getLastPathComponent() instanceof DefaultMutableTreeNode node &&
                node.getUserObject() instanceof DirectoryDto dir &&
                !(dir instanceof TestCasesMainDirectoryDto) &&
                !(dir instanceof TestRunsMainDirectoryDto)
        );
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }


}