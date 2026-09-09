package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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
            "High",
            1,
            JBColor.RED.brighter().brighter(),
            true,
            ReportEmphasis.ALARMING
    ),

    MEDIUM(
            "Medium",
            2,
            JBColor.BLUE.brighter(),
            true,
            ReportEmphasis.CAUTIONARY
    ),

    LOW(
            "Low",
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
    // Always true here, and still a real extension point rather than dead: it is
    // read through method references (Priority::isActive, Group::isActive) that a
    // search for isActive() does not find, and Group.UNASSIGNED in the third enum
    // of the set is genuinely inactive. Deleting it once already reached a
    // failing compile - remove it from all three enums together or not at all
    // (#66, E3).
    private final boolean active;
    /**
     * How loudly this reads in a report - see {@link ReportEmphasis}.
     */
    private final @NotNull ReportEmphasis emphasis;

}
