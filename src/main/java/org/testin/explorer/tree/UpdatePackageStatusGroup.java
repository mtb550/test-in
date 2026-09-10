package org.testin.explorer.tree;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.PackageStatus;

import java.util.Arrays;

/**
 * Every status a package can be set to, as one thing the platform owns (#119).
 * <p>
 * The shape the test project and test set groups explain: the children are
 * generated from {@link PackageStatus}, so the enum stays the one place a status
 * is declared, and the group is compact so the entries sit directly in the
 * Actions menu where they always have.
 */
public class UpdatePackageStatusGroup extends DefaultActionGroup {

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
        return Arrays.stream(PackageStatus.values())
                .map(UpdatePackageStatusAction::new)
                .toArray(AnAction[]::new);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
