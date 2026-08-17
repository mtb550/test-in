package org.testin.model;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Execution status of a test case. Constants carry their own presentation and
 * action wiring: label, icon, shortcut, extra-dialog flag.
 * <p>
 * So adding a status means adding one constant here. It does not mean writing a
 * new action class, wiring a new shortcut, and remembering a new menu entry
 * (see issue #37).
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
            false
    ),

    FAILED(
            "FF0000",
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            JBColor.RED.darker(),
            "Failed",
            new MenuEntry(AllIcons.Actions.Cancel, KeyStroke.getKeyStroke(KeyEvent.VK_F, 0)),
            true
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            JBColor.ORANGE,
            "Blocked",
            new MenuEntry(AllIcons.Actions.Pause, KeyStroke.getKeyStroke(KeyEvent.VK_B, 0)),
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
            // No menu entry, so not on the menu: the run owns this status. It
            // means "queued to run", and a run only clears it on completion, so a
            // tester setting it afterward leaves a state nothing reconciles.
            null,
            false
    ),

    UNTESTED(
            "808080",
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            JBColor.GRAY.brighter(),
            "Untested",
            // Off the menu, and the plugin sets it: a tester gives one of three
            // verdicts — passed, failed or blocked — and anything still pending
            // when the run completes or closes becomes untested by itself. It is
            // the record of a case the run never reached, not a verdict someone
            // chose, so there is nothing for a menu entry to apply.
            null,
            false
    );

    private final @NotNull String hex;
    private final @NotNull String displayText;
    private final @NotNull SimpleTextAttributes style;
    private final @NotNull Color rowColor;
    private final @NotNull String label;

    /**
     * How this status appears on the status menu, or null when the menu does not
     * offer it. Null is the whole answer: there used to be a separate
     * addedToMenu flag beside this, which allowed the two states that cannot
     * mean anything — an entry that is never shown, and a status offered with no
     * key to apply it. PENDING was the first: off the menu, yet carrying an icon
     * and a key nothing could reach.
     */
    private final @Nullable MenuEntry menuEntry;

    /**
     * True when applying this status first collects details in a dialog (FAILED).
     */
    private final boolean collectsFailureDetails;

    /**
     * True when a tester chose this status. The three the menu offers are the
     * three verdicts, and PENDING and UNTESTED are the two the run sets for
     * itself - which is what the null menu entry already says. Asked by name
     * here so no caller has to know the two facts coincide.
     */
    public boolean isVerdict() {
        return menuEntry != null;
    }

    /**
     * The status menu's presentation of a status it offers: the icon of its
     * action and the key that applies it. Only the statuses on the menu have
     * one, which is what makes the null meaningful.
     */
    public record MenuEntry(@NotNull Icon icon, @NotNull KeyStroke shortcut) {
    }
}
