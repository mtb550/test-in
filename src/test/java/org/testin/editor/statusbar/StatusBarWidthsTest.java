package org.testin.editor.statusbar;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * How the status bar shares its width out.
 * <p>
 * Asserted as arithmetic rather than through a laid-out component, because that
 * is where it went wrong: twice, and neither time was visible until the editor
 * was made narrow. The invariants below are the ones a screenshot cannot check -
 * that two regions never overlap, and that the sentence is never laid out at
 * zero width while there is room for it.
 */
public class StatusBarWidthsTest {

    private static final int ARROWS = 160;
    private static final int FIGURES = 420;

    /**
     * Everything placed fits, and the arrows never begin before the sentence has
     * had its room or end after the figures have begun.
     */
    private static void assertCoherent(final int inner, final @NotNull StatusBar.Widths widths) {
        final @NotNull String at = "inner=" + inner + " -> " + widths;

        assertTrue(widths.arrows() >= 0 && widths.figures() >= 0 && widths.arrowsAt() >= 0, at);
        assertTrue(widths.arrows() + widths.figures() <= inner, "more placed than there is room for: " + at);
        assertTrue(widths.arrowsAt() + widths.arrows() <= inner - widths.figures(),
                "the arrows run into the figures, which is what paints over them: " + at);
        assertTrue(widths.arrowsAt() <= inner, "the sentence is given more room than the bar has: " + at);
    }

    /**
     * The failure this class exists for. Every width from nothing to comfortable,
     * so a bar being dragged narrow passes through all of them.
     */
    @Test
    public void nothingEverOverlapsAtAnyWidth() {
        for (int inner = 0; inner <= 1400; inner++) {
            assertCoherent(inner, StatusBar.budget(inner, ARROWS, FIGURES));
        }
    }

    @Test
    public void nothingOverlapsWhenThereAreNoFiguresToPlace() {
        for (int inner = 0; inner <= 1400; inner++) {
            assertCoherent(inner, StatusBar.budget(inner, ARROWS, 0));
        }
    }

    /**
     * A tester who can see the bar can read the sentence. It shortens; it does
     * not disappear while there is room for it, which is the defect that reached
     * a screenshot.
     */
    @Test
    public void theSentenceKeepsItsFloorWheneverTheBarCanAffordOne() {
        for (int inner = 200; inner <= 1400; inner++) {
            final @NotNull StatusBar.Widths widths = StatusBar.budget(inner, ARROWS, FIGURES);

            assertTrue(widths.arrowsAt() > 0, "the sentence was given nothing at inner=" + inner);
        }
    }

    /**
     * Everything asked for fits, so everything asked for is granted, and the
     * arrows sit in the middle of the bar rather than in the room left over.
     */
    @Test
    public void aWideBarGrantsEveryRegionItsWidthAndCentersTheArrows() {
        final int inner = 1200;
        final @NotNull StatusBar.Widths widths = StatusBar.budget(inner, ARROWS, FIGURES);

        assertEquals(widths.arrows(), ARROWS);
        assertEquals(widths.figures(), FIGURES);
        assertEquals(widths.arrowsAt(), (inner - ARROWS) / 2, "centered in the bar, not between the two ends");
    }

    /**
     * The figures are what a narrow bar loses, and they are lost off the end
     * rather than by being drawn over.
     */
    @Test
    public void theFiguresGiveRoomUpBeforeTheArrowsOrTheSentenceDo() {
        final @NotNull StatusBar.Widths tight = StatusBar.budget(500, ARROWS, FIGURES);

        assertEquals(tight.arrows(), ARROWS, "the arrows are the last thing to shrink");
        assertTrue(tight.figures() < FIGURES, "the figures should have given room up at 500");
        assertTrue(tight.arrowsAt() >= 150, "the sentence should still hold its floor at 500");
    }

    /**
     * A bar with almost nothing to give still answers, and answers something
     * that can be laid out.
     */
    @Test
    public void aBarTooNarrowForAnythingStillAnswersCoherently() {
        for (final int inner : new int[]{0, 1, 2, 40, 159, 160, 161}) {
            assertCoherent(inner, StatusBar.budget(inner, ARROWS, FIGURES));
        }

        assertEquals(StatusBar.budget(0, ARROWS, FIGURES), new StatusBar.Widths(0, 0, 0));
    }
}
