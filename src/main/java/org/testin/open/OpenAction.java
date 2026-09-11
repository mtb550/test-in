package org.testin.open;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.editor.TestinEditors;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import java.util.List;


/**
 * UC-TREE-PANEL-005, UC-TREE-PANEL-006.
 * <p>
 * Declared in {@code plugin.xml} (#119) so Find Action offers it, and with no
 * {@code keyboard-shortcut} of its own: ENTER on a tree is that tree's gesture,
 * not a command, and a global ENTER would fire in every editor in the IDE. The
 * tree registers it on the declared instance instead, which keeps one action
 * behind both the menu entry and the key.
 */
public class OpenAction extends DumbAwareAction {

    // UC-TREE-PANEL-005, UC-TREE-PANEL-006, Rule-TREE-PANEL-022
    public static void execute(final @NotNull Project p, final @NotNull List<DirectoryDto> selected) {
        // Unresolvable nodes are not in the list at all, and one that cannot be
        // opened is skipped: the rest of the selection still opens.
        selected.stream()
                .filter(DirectoryDto::isOpenableInEditor)
                .forEach(dir -> {
                    Logger.info("open: " + dir.getPath());
                    Services.getInstance(p, TestinEditors.class).open(p, dir);
                });
    }

    // UC-TREE-PANEL-005, UC-TREE-PANEL-006
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        execute(p, TestinData.selectedNodes(e));
    }

    // UC-TREE-PANEL-005, Rule-TREE-PANEL-022
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.selectedNodes(e).stream()
                .anyMatch(DirectoryDto::isOpenableInEditor));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
