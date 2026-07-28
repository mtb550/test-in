package org.testin.actions.nodeCreator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.Dialogs.createNodes.CreateNodesDialog;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTreeNode extends DumbAwareAction {
    @Getter
    private final @NotNull ProjectPanel projectPanel;

    @Getter
    private final @NotNull SimpleTree tree;

    public CreateTreeNode(final @NotNull ProjectPanel projectPanel, final @NotNull SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.projectPanel = projectPanel;
        this.tree = tree;
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;
        final Project project = e.getProject();

        final DirectoryDto parentDir = Services.getInstance(project, Tools.class).getCurrentSelectedDirectory(tree);
        final TreePath path = tree.getSelectionPath();

        if (path == null || parentDir == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        new CreateNodesDialog(project, parentDir.getMenu(), (name, directoryType, cg) -> {

            if (name.isEmpty()) return;
            DirectoryDto dir = null;
            final Path newDirPath = parentDir.getPath().resolve(name);

            if (directoryType.getAction() != null)
                dir = directoryType.getAction().execute(this, project, name, parentNode, parentDir, newDirPath);

            else
                Logger.info("No creation logic defined for type: " + directoryType);

            if (cg.isSelected() && directoryType.getAction() != null) {

                if (directoryType == DirectoryType.TSP) {
                    GeneratorType.CREATE_JAVA_PACKAGE.getAction().execute(project, dir);
                    return;
                }

                if (directoryType == DirectoryType.TS) {
                    GeneratorType.CREATE_JAVA_CLASS.getAction().execute(project, dir);
                    //return;
                }

            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        if (e.getProject() == null) return;

        DirectoryDto parentDir = Services.getInstance(e.getProject(), Tools.class).getCurrentSelectedDirectory(tree);

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