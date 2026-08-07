package org.testin.nodeCreator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.nodeCreator.dialogs.CreateNodesDialog;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTreeNodeAction extends DumbAwareAction {

    private final @NotNull Project p;

    @Getter
    private final @NotNull ProjectPanel projectPanel;

    @Getter
    private final @NotNull SimpleTree tree;

    public CreateTreeNodeAction(final @NotNull Project p, final @NotNull ProjectPanel projectPanel, final @NotNull SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.p = p;
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final DirectoryDto parentDir = Services.getInstance(p, Tools.class).getCurrentSelectedDirectory(tree);
        final TreePath path = tree.getSelectionPath();

        if (path == null || parentDir == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        new CreateNodesDialog(p, parentDir.getMenu(), (s, dt, cg) -> {

            if (s.isEmpty()) return;
            final Path newDirPath = parentDir.getPath().resolve(s);

            if (dt.getAction() == null) {
                Logger.info("No creation logic defined for type: " + dt);
                return;
            }

            DirectoryDto dir = dt.getAction().apply(p).execute(tree, s, parentNode, parentDir, newDirPath);

            if (dt == DirectoryType.TS)
                Services.getInstance(p, EditorUtil.class).open(p, dir);

            if (cg.isSelected() && dt.getCodeGenerator() != null)
                dt.getCodeGenerator().execute(p, dir);

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {

        DirectoryDto parentDir = Services.getInstance(p, Tools.class).getCurrentSelectedDirectory(tree);

        if (parentDir == null || parentDir instanceof TestProjectDirectoryDto) {
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