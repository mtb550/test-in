package org.testin.open;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.projectPanel.tree.TreeValueUtil;
import org.testin.services.Services;
import org.testin.util.EditorUtil;
import org.testin.util.Shortcuts;

import javax.swing.tree.TreePath;

public class OpenAction extends DumbAwareAction {
    private final @NotNull Project p;
    private final @NotNull SimpleTree tree;

    public OpenAction(final @NotNull Project p, final @NotNull SimpleTree tree) {
        super("Open", "Open selected test sets or runs", AllIcons.Actions.MenuOpen);
        this.p = p;
        this.tree = tree;

        this.registerCustomShortcutSet(Shortcuts.Enter.getCustomShortcut(), tree);
    }

    public void execute(final @NotNull Project p) {
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return;

        for (TreePath path : paths) {
            final DirectoryDto directoryDto = TreeValueUtil.directoryOf(path.getLastPathComponent());
            // Skip unresolvable nodes but keep opening the rest of the selection.
            if (directoryDto == null || !directoryDto.isOpenableInEditor()) continue;

            Logger.info("open: " + directoryDto.getPath());
            Services.getInstance(p, EditorUtil.class).openIfNotOpen(p, directoryDto);
        }
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute(p);
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        TreePath[] paths = tree.getSelectionPaths();
        boolean shouldEnable = false;

        if (paths != null) {
            for (TreePath path : paths) {
                Object value = TreeValueUtil.valueOf(path.getLastPathComponent());
                if (value instanceof DirectoryDto dir && dir.isOpenableInEditor()) {
                    shouldEnable = true;
                    break;
                }
            }
        }

        e.getPresentation().setEnabled(shouldEnable);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
