package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Lifecycle status of a test run. Constants carry their own icon, keyboard
 * shortcut and transition, so status rules live here instead of being
 * re-implemented as if-chains at the call sites (issue #37).
 * <p>
 * The icon is what the project tree draws for a run node, so a cycle's state
 * is readable without opening it.
 */
@Getter
@AllArgsConstructor
public enum TestRunStatus {
    CREATED(
            "Created",
            Shortcuts.NO_KEY,
            AllIcons.General.Add
    ),

    IN_PROGRESS(
            "In Progress",
            Shortcuts.NO_KEY,
            AllIcons.Actions.BuildAutoReloadChanges
    ),

    COMPLETED(
            "Completed",
            KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
            AllIcons.Toolwindows.ToolWindowCoverage
    ),

    ASSIGNED(
            "Assigned",
            KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
            AllIcons.Gutter.ExtAnnotation
    ), //todo, later, use XML to add tester's name dynamic

    CLOSED(
            "Closed",
            KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
            AllIcons.Actions.Cancel
    );

    private final @NotNull String label;

    /**
     * The key that moves a run to this status, and {@link Shortcuts#NO_KEY} for
     * the statuses no key reaches.
     */
    private final @NotNull KeyStroke shortcut;
    private final @NotNull Icon icon;

    /**
     * True when the run has reached a terminal state (completed or closed).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLOSED;
    }

    public @NotNull String getShortcutText() {
        return Shortcuts.shortcutText(shortcut);
    }

    public void bindShortcut(final @NotNull JComponent component, final @NotNull Runnable onAction) {
        if (Shortcuts.isNoKey(shortcut)) return;

        new DumbAwareAction() {
            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                onAction.run();
            }
        }.registerCustomShortcutSet(Shortcuts.customShortcut(shortcut), component);
    }
}
