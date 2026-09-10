package org.testin.testset;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.model.TestSetStatus;

import java.util.Arrays;

/**
 * Every status a test set can be set to, as one thing the platform owns (#119).
 * <p>
 * The shape its test project sibling explains one file over: the children are
 * generated from {@link TestSetStatus}, so the enum stays the one place a status
 * is declared, and the group is compact so the entries sit directly in the
 * Actions menu where they always have.
 */
public class UpdateTestSetStatusGroup extends DefaultActionGroup {

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public AnAction @NotNull [] getChildren(final @Nullable AnActionEvent e) {
        return Arrays.stream(TestSetStatus.values())
                .map(UpdateTestSetStatusAction::new)
                .toArray(AnAction[]::new);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
