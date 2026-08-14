package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Execution status of a test case. Constants carry their own presentation and
 * action wiring (label, icon, shortcut, extra-dialog flag), so adding a status
 * means adding one constant here — not writing a new action class, wiring a
 * new shortcut, and remembering a new menu entry (see issue #37).
 */
@Getter
@AllArgsConstructor
public enum TestStatus {
    PASSED(
            "008000",
            " [Passed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN),
            JBColor.GREEN,
            "Passed",
            new MenuEntry(AllIcons.Actions.Checked, KeyStroke.getKeyStroke(KeyEvent.VK_P, 0)),
            true,
            false
    ),

    FAILED(
            "FF0000",
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            JBColor.RED.darker(),
            "Failed",
            new MenuEntry(AllIcons.Actions.Cancel, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)),
            true,
            true
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            JBColor.ORANGE,
            "Blocked",
            new MenuEntry(AllIcons.Actions.Pause, KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
            true,
            false
    ),

    PENDING(
            "808080",
            " [Pending]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            // Lazy because it comes from the theme: resolved at class-load time it
            // would keep the color of whichever theme happened to be active then.
            JBColor.lazy(UIUtil::getContextHelpForeground),
            "Pending",
            new MenuEntry(AllIcons.Actions.Restart, KeyStroke.getKeyStroke(KeyEvent.VK_N, 0)),
            // Off the menu: the run owns it. It means "queued to run", and a run
            // only clears it on completion, so a tester setting it afterward
            // leaves a state nothing reconciles.
            false,
            false
    ),

    UNTESTED(
            "808080",
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            JBColor.GRAY.brighter(),
            "Untested",
            new MenuEntry(AllIcons.Actions.Rollback, KeyStroke.getKeyStroke(KeyEvent.VK_U, 0)),
            // On the menu as "clear my verdict": marking a case Failed and then
            // finding you tested the wrong build had no way back.
            true,
            false
    );

    private final @NotNull String hex;
    private final @NotNull String displayText;
    private final @NotNull SimpleTextAttributes style;
    private final @NotNull Color rowColor;
    private final @NotNull String label;

    /**
     * How this status appears on the status menu. One field rather than two, so
     * an icon without a shortcut - a status offered with no key to apply it -
     * cannot be written.
     */
    private final @NotNull MenuEntry menuEntry;

    /**
     * Whether the status menu offers it. Separate from the entry above so that
     * offering a status is one flag rather than inventing a presentation for it.
     */
    private final boolean addedToMenu;

    /**
     * True when applying this status first collects details in a dialog (FAILED).
     */
    private final boolean collectsFailureDetails;

    /**
     * The status menu's presentation of a status it offers: the icon of its
     * action and the key that applies it. Only the statuses on the menu have
     * one, which is what makes the null meaningful.
     */
    public record MenuEntry(@NotNull Icon icon, @NotNull KeyStroke shortcut) {
    }
}
