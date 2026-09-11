package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;
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
public enum TestRunStatus implements MenuItem {
    CREATED(
            Bundle.message("status.run.created"),
            Shortcuts.NO_KEY,
            AllIcons.General.Add,
            Stage.MADE,
            SetBy.TESTIN
    ),

    IN_PROGRESS(
            Bundle.message("status.run.in.progress"),
            Shortcuts.NO_KEY,
            AllIcons.Actions.BuildAutoReloadChanges,
            Stage.RUNNING,
            SetBy.TESTIN
    ),

    COMPLETED(
            Bundle.message("status.run.completed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_2, 0),
            AllIcons.Toolwindows.ToolWindowCoverage,
            Stage.OVER,
            SetBy.TESTER
    ),

    ASSIGNED(
            Bundle.message("status.run.assigned"),
            KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
            AllIcons.Gutter.ExtAnnotation,
            Stage.HANDED_OUT,
            SetBy.TESTER
    ), //todo, later, use XML to add tester's name dynamic

    CLOSED(
            Bundle.message("status.run.closed"),
            KeyStroke.getKeyStroke(KeyEvent.VK_3, 0),
            AllIcons.Actions.Cancel,
            Stage.OVER,
            SetBy.TESTER
    );

    /**
     * UC-TREE-PANEL-020, Rule-TREE-PANEL-068.
     * <p>
     * Who puts a run in this status. Two of the five are the run's own record of
     * itself - Created when it is made, In Progress when execution starts - and
     * offering them on the Set Status menu invited a tester to declare something
     * that had either happened or not (#186).
     */
    private enum SetBy { TESTER, TESTIN }

    /**
     * UC-TREE-PANEL-020, Rule-TREE-PANEL-092.
     * <p>
     * How far through its life the run is. A number rather than the declaration
     * order, which is not the lifecycle: the constants are declared in the order
     * the menu once drew them.
     * <p>
     * Completed and Closed share the last one. They are two ways of being over,
     * not one after the other, and a run in either is signed off - which is
     * already why Set Status is gray on it.
     */
    private static final class Stage {
        private static final int MADE = 0;
        private static final int HANDED_OUT = 1;
        private static final int RUNNING = 2;
        private static final int OVER = 3;
    }

    private final @NotNull String label;

    /**
     * The key that moves a run to this status, and {@link Shortcuts#NO_KEY} for
     * the statuses no key reaches.
     */
    private final @NotNull KeyStroke shortcut;
    private final @NotNull Icon icon;

    @Getter(AccessLevel.NONE)
    private final int stage;

    @Getter(AccessLevel.NONE)
    private final @NotNull SetBy setBy;

    /**
     * UC-TREE-PANEL-020, Rule-TREE-PANEL-068, Rule-TREE-PANEL-092.
     * <p>
     * Whether a tester can move this run to that status.
     * <p>
     * Two questions in one answer, because they are one question at the menu: is
     * it a status a tester sets at all, and is it forward of where the run is
     * now. A run could be moved from Assigned back to Created, and from In
     * Progress back to Assigned, which un-says something that has happened
     * (#186).
     */
    public boolean canBeSetFrom(final @NotNull TestRunStatus current) {
        return setBy == SetBy.TESTER && stage > current.stage;
    }

    /**
     * UC-REPORT-001, Rule-REPORT-016.
     * <p>
     * A report is about what a run recorded, and a run still going is still
     * recording - so the document would describe a state that has already moved
     * on (#253).
     */
    public boolean isReportable() {
        return this != IN_PROGRESS;
    }

    /**
     * True when the run has reached a terminal state (completed or closed).
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CLOSED;
    }

    public @NotNull String getShortcutText() {
        return Shortcuts.shortcutText(shortcut);
    }

    /**
     * The same word {@link #getLabel()} gives, under the name a menu row is asked
     * for. {@code getLabel} is what a hundred callers already say for a display
     * word and {@code getName} is what {@link org.testin.model.StatusBarItem}
     * calls it, so one of the two has to bridge - and a status is read far more
     * often than it is put on a menu.
     */
    @Override
    public @NotNull String getName() {
        return label;
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
