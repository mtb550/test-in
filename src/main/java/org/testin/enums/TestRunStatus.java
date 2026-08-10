package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
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
            KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
            AllIcons.Actions.Checked
    ),

    ASSIGNED(
            "Assigned",
            KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
            AllIcons.General.User
    ), //todo, later, use xml to add tester's name dynamic

    CLOSED(
            "Closed",
            KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
            AllIcons.Actions.Cancel
    );

    private static final Map<TestRunStatus, TestRunStatus> TRANSITIONS = Map.of(
            CREATED, IN_PROGRESS,
            ASSIGNED, IN_PROGRESS,
            IN_PROGRESS, COMPLETED
    );
    private final String label;
    private final KeyStroke shortcut;
    private final Icon icon;

    public String getShortcutText() {
        return Optional.ofNullable(shortcut)
                .map(Tools::shortcutText)
                .orElse("");
    }

    public void bindShortcut(final JComponent component, final Runnable onAction) {
        if (shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    onAction.run();
                }
            }.registerCustomShortcutSet(Tools.customShortcut(shortcut), component);
        }
    }
}