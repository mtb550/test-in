package org.testin.testproject;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.ProjectStatus;

import java.util.Arrays;

/**
 * Every status a test project can be set to, as one thing the platform owns
 * (#119).
 * <p>
 * A group rather than an action, because there is one entry per {@link
 * ProjectStatus} and a status is a constant on an enum: writing an {@code
 * <action>} element per constant would put the list in {@code plugin.xml} as
 * well as in the enum, and the day somebody adds a fourth status the menu would
 * be missing it with nothing to say so (#175, C9). The children are generated
 * from {@code values()}, so the enum stays the one place a status is declared.
 * <p>
 * Compact rather than a submenu - {@code popup="false"} in the descriptor - so
 * the statuses sit directly in the Actions menu where they always have, and each
 * child hides itself on a node that is not a test project.
 */
public class UpdateTestProjectStatusGroup extends DefaultActionGroup {

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
        return Arrays.stream(ProjectStatus.values())
                .map(UpdateTestProjectStatusAction::new)
                .toArray(AnAction[]::new);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
