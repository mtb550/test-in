package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * How much a bug hurts, as the tester judged it when the case failed.
 * <p>
 * The colors read as a traffic light rather than as another priority scale, on
 * purpose: the card already draws the test case's priority and the bug's
 * priority, and those two declare the same red, blue and gray as each other. A
 * third set from the same palette would make the badge row a wall of similar
 * pills. The platform constants are used as they are, both theme halves
 * included, and {@code Shared} derives the badge text color from whichever half
 * is showing - so the light values stay readable without being darkened here.
 */
@Getter
@AllArgsConstructor
public enum BugSeverity {
    /**
     * No severity, because the case did not fail. The color is never painted:
     * a badge is not drawn for a blank value.
     */
    EMPTY(
            "",
            JBColor.background(),
            ReportEmphasis.MUTED
    ),

    BLOCKER(
            "Blocker",
            JBColor.RED,
            ReportEmphasis.ALARMING
    ),

    MAJOR(
            "Major",
            JBColor.ORANGE,
            ReportEmphasis.CAUTIONARY
    ),

    MINOR(
            "Minor",
            JBColor.YELLOW,
            ReportEmphasis.MUTED
    ),

    ENHANCEMENT(
            "Enhancement",
            JBColor.GREEN.brighter().brighter(),
            ReportEmphasis.MUTED
    );

    /**
     * What a tester can choose, in the order declared above.
     * <p>
     * EMPTY is what a bug nobody filled in carries - a persistence default rather
     * than a severity anyone means - so it is the one constant not offered.
     * Derived rather than listed, because it was listed: the four choices were
     * typed out in {@code FailedResultDialog}, so a fifth severity added here
     * would have been missing from the only dialog that sets one, silently
     * (#175, C14).
     */
    public static final @NotNull List<BugSeverity> CHOICES =
            Arrays.stream(values()).filter(severity -> severity != EMPTY).toList();

    /**
     * What to show for a stored value, where EMPTY means nobody has chosen yet.
     * <p>
     * Enhancement, because it is the mildest thing a filed bug can be and a
     * dialog that opens on the worst one invites a tester to leave it there.
     */
    public static @NotNull BugSeverity orDefault(final @NotNull BugSeverity stored) {
        return stored == EMPTY ? ENHANCEMENT : stored;
    }

    private final @NotNull String label;
    private final @NotNull Color color;
    /**
     * How loudly this reads in a report - see {@link ReportEmphasis}.
     */
    private final @NotNull ReportEmphasis emphasis;

}
