package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;
import org.testin.util.autoGenerator.GeneratorType;
import org.testin.util.logger.Log;

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
        final DirectoryDto parentDir = Tools.getInstance().getCurrentSelectedDirectory(tree);

        TreePath path = tree.getSelectionPath();
        if (path == null) return;

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        final Project project = e.getProject();
        new CreateNodesDialog(project, parentDir.getMenu(), (name, directoryType, codeGenerator) -> {

            if (name == null || name.isEmpty()) return;
            DirectoryDto dir = null;
            final Path newDirPath = parentDir.getPath().resolve(name);

            if (directoryType != null && directoryType.getAction() != null)
                dir = directoryType.getAction().execute(this, e.getProject(), name, parentNode, parentDir, newDirPath);

            else
                Log.info("No creation logic defined for type: " + directoryType);

            if (codeGenerator != null && codeGenerator.isSelected() && directoryType != null && directoryType.getAction() != null) {

                if (directoryType == DirectoryType.TSP) {
                    GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(null, dir.getFqcn());
                    return;
                }

                if (directoryType == DirectoryType.TS) {
                    GeneratorType.CREATE_TEST_SET.getAction().execute(null, dir.getFqcn());
                }

            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        DirectoryDto parentDir = Tools.getInstance().getCurrentSelectedDirectory(tree);

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