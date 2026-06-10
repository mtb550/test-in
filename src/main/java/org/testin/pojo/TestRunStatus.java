package org.testin.pojo;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.KeyboardSet;

import javax.swing.*;
import java.util.Optional;

@Getter
public enum TestRunStatus {
    CREATED(
            "Created",
            null,
            AllIcons.Nodes.Test
    ),

    IN_PROGRESS(
            "In Progress",
            null,
            AllIcons.Actions.Execute
    ),

    COMPLETED(
            "Completed",
            KeyboardSet.StatusCompleted,
            AllIcons.Actions.Checked
    ),

    ASSIGNED(
            "Assigned",
            KeyboardSet.StatusAssigned,
            AllIcons.General.User
    ), //todo, later, use xml to add tester's name dynamic

    CLOSED(
            "Closed",
            KeyboardSet.StatusClosed,
            AllIcons.Actions.Cancel
    );

    private final String label;
    private final KeyboardSet keyboardSet;
    private final Icon icon;

    TestRunStatus(final String label, final KeyboardSet keyboardSet, final Icon icon) {
        this.label = label;
        this.keyboardSet = keyboardSet;
        this.icon = icon;
    }

    public String getShortcutText() {
        return Optional.ofNullable(keyboardSet)
                .map(KeyboardSet::getShortcutText)
                .orElse("");
    }

    public void bindShortcut(final JComponent component, final Runnable onAction) {
        if (keyboardSet != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    onAction.run();
                }
            }.registerCustomShortcutSet(keyboardSet.getCustomShortcut(), component);
        }
    }
}