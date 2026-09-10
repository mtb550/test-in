package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum BugPriority {
    EMPTY(
            "",
            0,
            JBColor.background(),
            true,
            ReportEmphasis.MUTED
    ),

    HIGH(
            Bundle.message("bug.priority.high"),
            1,
            JBColor.RED.brighter().brighter(),
            true,
            ReportEmphasis.ALARMING
    ),

    MEDIUM(
            Bundle.message("bug.priority.medium"),
            2,
            JBColor.BLUE.brighter(),
            true,
            ReportEmphasis.CAUTIONARY
    ),

    LOW(
            Bundle.message("bug.priority.low"),
            3,
            JBColor.GRAY.brighter(),
            true,
            ReportEmphasis.MUTED
    );

    /**
     * What a tester can choose, in the order declared above - everything but
     * EMPTY, for the reason {@link BugSeverity#CHOICES} gives (#175, C14).
     */
    public static final @NotNull List<BugPriority> CHOICES =
            Arrays.stream(values()).filter(priority -> priority != EMPTY).toList();

    /**
     * What to show for a stored value, where EMPTY means nobody has chosen yet.
     */
    public static @NotNull BugPriority orDefault(final @NotNull BugPriority stored) {
        return stored == EMPTY ? LOW : stored;
    }

    private final @NotNull String label;
    private final int value;
    private final @NotNull Color color;
    // Always true here, and read through a method reference (Priority::isActive)
    // that a search for isActive() does not find - PrioritySection filters on it,
    // so deleting it reaches a failing compile rather than a warning.
    //
    // Group carried the same flag and no longer does: every group can be typed
    // now, so nothing read it (#200). This one is on its own until a priority is
    // retired, which is the case it exists for.
    private final boolean active;
    /**
     * How loudly this reads in a report - see {@link ReportEmphasis}.
     */
    private final @NotNull ReportEmphasis emphasis;

}
