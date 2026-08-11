package org.testin.enums;

import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Execution status of a test case. Constants carry their own presentation and
 * action wiring (label, icon, shortcut, extra-dialog flag), so adding a status
 * means adding one constant here — not writing a new action class, wiring a
 * new shortcut, and remembering a new menu entry (see issue #37).
 */
@Getter
public enum TestStatus {
    PASSED(
            "008000",
            " [Passed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GREEN),
            JBColor.GREEN,
            "Passed",
            AllIcons.Actions.Checked,
            KeyStroke.getKeyStroke(KeyEvent.VK_P, 0),
            false
    ),

    FAILED(
            "FF0000",
            " [Failed]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED),
            JBColor.RED.darker(),
            "Failed",
            AllIcons.Actions.Cancel,
            KeyStroke.getKeyStroke(KeyEvent.VK_F, 0),
            true
    ),

    BLOCKED(
            "FFA500",
            " [Blocked]",
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE),
            JBColor.ORANGE,
            "Blocked",
            AllIcons.Actions.Pause,
            KeyStroke.getKeyStroke(KeyEvent.VK_B, 0),
            false
    ),

    PENDING(
            "808080",
            " [Pending]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            null,
            "Pending"
    ),

    UNTESTED(
            "808080",
            " [Untested]",
            SimpleTextAttributes.REGULAR_ATTRIBUTES,
            JBColor.GRAY.brighter(),
            "Untested"
    );

    private final @NotNull String hex;
    private final @NotNull String displayText;
    private final @NotNull SimpleTextAttributes style;
    private final @Nullable Color rowColor;
    private final @NotNull String label;

    /** Icon of the set-status action; null for statuses the user cannot set from the menu. */
    private final @Nullable Icon icon;

    /** Keyboard shortcut of the set-status action; null when not user-settable. */
    private final @Nullable KeyStroke shortcut;

    /** True when applying this status first collects details in a dialog (FAILED). */
    private final boolean collectsFailureDetails;

    TestStatus(final @NotNull String hex, final @NotNull String displayText, final @NotNull SimpleTextAttributes style,
               final @Nullable Color rowColor, final @NotNull String label) {
        this(hex, displayText, style, rowColor, label, null, null, false);
    }

    TestStatus(final @NotNull String hex, final @NotNull String displayText, final @NotNull SimpleTextAttributes style,
               final @Nullable Color rowColor, final @NotNull String label, final @Nullable Icon icon,
               final @Nullable KeyStroke shortcut, final boolean collectsFailureDetails) {
        this.hex = hex;
        this.displayText = displayText;
        this.style = style;
        this.rowColor = rowColor;
        this.label = label;
        this.icon = icon;
        this.shortcut = shortcut;
        this.collectsFailureDetails = collectsFailureDetails;
    }

    /** True when the user can set this status from the context menu. */
    public boolean isUserSettable() {
        return icon != null;
    }
}
