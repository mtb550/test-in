package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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
            JBColor.GREEN,
            ReportEmphasis.MUTED
    );

    private final @NotNull String name;
    private final @NotNull Color color;
    /**
     * How loudly this reads in a report - see {@link ReportEmphasis}.
     */
    private final @NotNull ReportEmphasis emphasis;

}
