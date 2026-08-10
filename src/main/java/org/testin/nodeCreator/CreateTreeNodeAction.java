package org.testin.nodeCreator;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.DirectoryType;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.nodeCreator.dialogs.CreateNodesDialog;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.KeyboardSet;
import org.testin.util.Tools;

import javax.swing.tree.TreePath;
import java.nio.file.Path;

public class CreateTreeNodeAction extends DumbAwareAction {

    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;
    private final @NotNull Tools tools;

    public CreateTreeNodeAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Create", "Create new node", AllIcons.General.Add);
        this.p = p;
        this.tree = tree;
        this.tools = Services.getInstance(p, Tools.class);
        this.registerCustomShortcutSet(KeyboardSet.CreateNode.getCustomShortcut(), tree);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {

        final DirectoryDto pDir = tools.getCurrentSelectedDirectory(tree);
        final TreePath path = tree.getSelectionPath();

        if (path == null || pDir == null) return;

        new CreateNodesDialog(p, pDir.getMenu(), (s, dt) -> {

            if (s.isEmpty()) return;
            final Path newDirPath = pDir.getPath().resolve(s);

            if (dt.getAction() == null) {
                Logger.info("No creation logic defined for type: " + dt);
                return;
            }

            DirectoryDto dir = dt.getAction().apply(p).execute(s, pDir, newDirPath);
            Services.getInstance(p, org.testin.projectPanel.ProjectPanel.class).getProjectTree().refresh();

            if (dt == DirectoryType.TS)
                Services.getInstance(p, EditorUtil.class).open(p, dir);

            if (dt.getCodeGenerator() != null)
                dt.getCodeGenerator().execute(p, dir);

        }).show();
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {

        DirectoryDto parentDir = tools.getCurrentSelectedDirectory(tree);

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
