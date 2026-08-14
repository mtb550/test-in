package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.util.Tools;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Optional;

/**
 * Lifecycle status of a test run. Constants carry their transitions and the
 * presentation of the advance action, so status rules live here instead of
 * being re-implemented as if-chains at the call sites (issue #37).
 */
@Getter
@AllArgsConstructor
public enum TestRunStatus {
    CREATED(
            "Created",
            null,
            AllIcons.Nodes.Test,
            "Start Execution",
            "Start execution of test cases",
            AllIcons.Nodes.Services
    ),

    IN_PROGRESS(
            "In Progress",
            null,
            AllIcons.Actions.Execute,
            "Complete Test Run",
            "Mark test run as completed",
            AllIcons.Actions.Checked
    ),

    COMPLETED(
            "Completed",
            KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
            AllIcons.Actions.Checked,
            "Start Execution",
            "Start execution of test cases",
            AllIcons.Nodes.Services
    ),

    ASSIGNED(
            "Assigned",
            KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
            AllIcons.General.User,
            "Start Execution",
            "Start execution of test cases",
            AllIcons.Nodes.Services
    ), //todo, later, use xml to add tester's name dynamic

    CLOSED(
            "Closed",
            KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
            AllIcons.Actions.Cancel,
            "Start Execution",
            "Start execution of test cases",
            AllIcons.Nodes.Services
    );

    private static final @NotNull Map<TestRunStatus, TestRunStatus> TRANSITIONS = Map.of(
            CREATED, IN_PROGRESS,
            ASSIGNED, IN_PROGRESS,
            IN_PROGRESS, COMPLETED
    );
    private final @NotNull String label;

    /**
     * Null for the statuses the tester cannot set directly from the keyboard.
     */
    private final @Nullable KeyStroke shortcut;
    private final @NotNull Icon icon;

    /**
     * Presentation of the advance action while the run is in this status.
     */
    private final @NotNull String advanceLabel;
    private final @NotNull String advanceDescription;
    private final @NotNull Icon advanceIcon;

    /**
     * The status the advance action moves this run to, or null when terminal.
     */
    public @Nullable TestRunStatus nextStatus() {
        return TRANSITIONS.get(this);
    }

    /**
     * True while the run can still advance to another status.
     */
    public boolean isAdvanceable() {
        return TRANSITIONS.containsKey(this);
    }

    /**
     * True when the run has reached a terminal state (completed or closed).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLOSED;
    }

    public @NotNull String getShortcutText() {
        return Optional.ofNullable(shortcut)
                .map(Tools::shortcutText)
                .orElse("");
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onAction) {
        if (shortcut != null) {
            new DumbAwareAction() {
                @Override
                public void actionPerformed(final @NotNull AnActionEvent e) {
                    onAction.run();
                }
            }.registerCustomShortcutSet(Tools.customShortcut(shortcut), component);
        }
    }
}
