package org.testin.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.Config;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.dto.dirs.DirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.ui.createNodes.CreateNodesDialog;
import org.testin.util.KeyboardSet;
import org.testin.util.autoGenerator.GeneratorType;

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

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

        if (!(parentNode.getUserObject() instanceof DirectoryDto parentDir)) return;

        new CreateNodesDialog(parentDir.getMenu(), (name, directoryType, codeGenerator) -> {
            if (name == null || name.isEmpty()) return;

            // todo, cover all regex -> dots, slashes ..etc
            final String processedName = name.replace("_", " ");
            final String javaPackageName = processedName.replace(" ", "").toLowerCase();

            final Path newDirPath = parentDir.getPath().resolve(name);

            if (directoryType != null && directoryType.getAction() != null)
                directoryType.getAction().execute(this, e.getProject(), name, parentNode, parentDir, newDirPath);
            else
                System.out.println("No creation logic defined for type: " + directoryType);

            System.out.println("start generate..");
            if (codeGenerator != null && codeGenerator.isSelected() && directoryType != null && directoryType.getAction() != null) {
                if (directoryType == DirectoryType.TSP) {
                    System.out.println("Selected directory type: " + directoryType);
                    GeneratorType.CREATE_TEST_SET_PACKAGE.getAction().execute(Config.getProject(), javaPackageName, path);
                    return;
                }

                if (directoryType == DirectoryType.TS) {
                    System.out.println("Selected directory type: " + directoryType);
                    GeneratorType.CREATE_TEST_SET.getAction().execute(Config.getProject(), javaPackageName, path);
                    return;
                }

                if (directoryType == DirectoryType.TRP) {
                    System.out.println("no need to generate, Selected directory type: " + directoryType);
                    return;
                }

                if (directoryType == DirectoryType.TR) {
                    System.out.println("no need to generate, Selected directory type: " + directoryType);
                }

            }

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        final TreePath path = tree.getSelectionPath();

        if (path == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) path.getLastPathComponent();

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