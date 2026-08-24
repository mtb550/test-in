package org.testin.open;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectTreeAction;
import org.testin.explorer.tree.TreeValueUtil;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.Shortcuts;


public class OpenAction extends AbstractProjectTreeAction {

    public OpenAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super(p, tree, "Open", "Open selected test sets or runs", AllIcons.Actions.MenuOpen);

        this.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), tree);
    }

    public void execute(final @NotNull Project p) {
        // Unresolvable nodes are not in the list at all, and one that cannot be
        // opened is skipped: the rest of the selection still opens.
        TreeValueUtil.selectedDirectories(tree.getSelectionPaths()).stream()
                .filter(DirectoryDto::isOpenableInEditor)
                .forEach(dir -> {
                    Logger.info("open: " + dir.getPath());
                    Services.getInstance(p, EditorUtil.class).open(p, dir);
                });
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute(p);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TreeValueUtil.selectedDirectories(tree.getSelectionPaths()).stream()
                .anyMatch(DirectoryDto::isOpenableInEditor));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
