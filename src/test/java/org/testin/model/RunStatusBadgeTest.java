package org.testin.model;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * A run status either draws a badge or it does not (#48).
 * <p>
 * The label and the color used to be separate fields - a {@code @NotNull} label
 * beside a {@code @Nullable} color - which let IDLE carry the label {@code ""}
 * for a badge it never draws, and made "has a label but no color" a state the
 * type allowed. They are one fact, so they are one field, and the card checks it
 * once instead of asking a predicate the checker could not follow.
 */
public class RunStatusBadgeTest {

    @Test
    public void idleDrawsNoBadge() {
        assertNull(RunStatus.IDLE.getBadge(), "a case nobody has run carries no badge");
    }

    @Test
    public void everyOtherStatusDrawsOne() {
        for (final RunStatus status : RunStatus.values()) {
            if (status == RunStatus.IDLE) continue;

            final RunStatus.Badge badge = status.getBadge();
            assertNotNull(badge, status + " should draw a badge");
            assertNotNull(badge.color(), status + " has a label, so it has a color to draw it in");
            assertFalse(badge.label().isBlank(), status + " has a visible label");
        }
    }

    @Test
    public void theLabelsAreTheOnesTheCardShows() {
        assertEquals(RunStatus.PASSED.getBadge().label(), "Passed");
        assertEquals(RunStatus.FAILED.getBadge().label(), "Failed");
        assertEquals(RunStatus.RUNNING.getBadge().label(), "Running");
    }

    @Test
    public void everyStatusHasAnIconAndTooltip() {
        for (final RunStatus status : RunStatus.values()) {
            assertNotNull(status.getIcon(), status + " needs an icon for the gutter");
            assertFalse(status.getTooltip().isBlank(), status + " needs a tooltip");
        }
    }
}
