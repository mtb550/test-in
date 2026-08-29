package org.testin.model;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What the run editor's status bar is handed to say how a run is going.
 * <p>
 * Asserted here rather than through the bar because the rules are not Swing
 * ones: which verdicts appear, in which order, under which names, and what
 * color each is drawn in. The bar only lays out what it is given.
 */
public class ResultAnalysisSegmentsTest {

    private static @NotNull TestRunSummary run(final long passed, final long failed, final long blocked, final long untested, final long removed) {
        return new TestRunSummary(passed + failed + blocked + untested + removed, passed, failed, blocked, untested, removed, 0, "");
    }

    private static @NotNull List<ResultAnalysis.Segment> of(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus run) {
        return ResultAnalysis.segments(summary, run);
    }

    /**
     * What a tester reads, as one line. The colors are asserted separately, so
     * the sentence assertions stay legible.
     */
    private static @NotNull String words(final @NotNull TestRunSummary summary) {
        return of(summary, TestRunStatus.IN_PROGRESS).stream()
                .map(ResultAnalysis.Segment::text)
                .collect(Collectors.joining(" · "));
    }

    @Test
    public void aRunNobodyHasTouchedSaysNothing() {
        assertTrue(of(TestRunSummary.EMPTY, TestRunStatus.IN_PROGRESS).isEmpty(),
                "nothing at all rather than a blank piece: the bar hides a run with nothing recorded the way it hides a zero duration");
    }

    /**
     * The rule {@code hasRemoved} already states, applied to all of them: a
     * verdict no case carries explains something that did not happen.
     */
    @Test
    public void aVerdictNoCaseCarriesIsLeftOut() {
        assertEquals(words(run(12, 0, 0, 108, 0)), "Passed 12 · Pending 108");
    }

    @Test
    public void theVerdictsReadInTheOrderTheEnumDeclaresThem() {
        assertEquals(words(run(1, 2, 3, 4, 0)), "Passed 1 · Failed 2 · Blocked 3 · Pending 4");
    }

    /**
     * The total on the other side of the bar counts a case deleted out from
     * under the run, so a line that left it out would not add up to the number
     * beside it.
     */
    @Test
    public void casesDeletedUnderTheRunAreCountedToo() {
        assertEquals(words(run(5, 0, 0, 0, 2)), "Passed 5 · Removed 2");
    }

    /**
     * The names are the statuses' own, so renaming one renames it in the reports,
     * the analysis dialog and the bar together.
     */
    @Test
    public void theNamesAreTheStatusesOwn() {
        assertEquals(words(run(1, 0, 0, 0, 0)), TestStatus.PASSED.getLabel() + " 1");
    }

    /**
     * One of the two colors the verdict declares - which one depends on the theme
     * the test happens to run under, and either is right.
     */
    @Test
    public void aVerdictIsPaintedInItsOwnColor() {
        final @NotNull Color painted = of(run(0, 3, 0, 0, 0), TestRunStatus.IN_PROGRESS).getFirst().color();

        assertTrue(painted.getRGB() == Color.decode("#" + ResultAnalysis.FAILED.getHexColor()).getRGB()
                        || painted.getRGB() == Color.decode("#" + ResultAnalysis.FAILED.getDarkHexColor()).getRGB(),
                "failed should be painted red, in whichever of its two reds suits the theme");
    }

    /**
     * <b>The defect this file exists for.</b> The color has to be one that is
     * asked which theme it is in every time it paints.
     * <p>
     * It was a hex literal written into an html string, chosen from the theme at
     * the moment the string was built. The label then held that string until
     * something handed it a new one, so a tester who switched theme kept the old
     * palette until they turned a page - light to dark left the untouched count
     * at a grey the enum's own comment calls very nearly the background. A fixed
     * {@link Color} here would bring the whole defect back with nothing failing.
     */
    @Test
    public void everyVerdictsColorFollowsTheThemeRatherThanBeingPickedOnce() {
        for (final ResultAnalysis.Segment segment : of(run(1, 1, 1, 1, 0), TestRunStatus.IN_PROGRESS)) {
            assertTrue(segment.color() instanceof JBColor,
                    segment.text() + " is drawn in a color that was resolved once and cannot follow a theme change");
        }
    }

    /**
     * A case deleted out from under the run is not a verdict anybody reached, so
     * it is counted in the bar's own text color rather than painted like one.
     */
    @Test
    public void aRemovedCaseIsCountedWithoutBeingPaintedAsAVerdict() {
        final @NotNull List<ResultAnalysis.Segment> segments = of(run(0, 0, 0, 0, 2), TestRunStatus.CLOSED);

        assertEquals(segments.size(), 1);
        assertEquals(segments.getFirst().text(), TestStatus.REMOVED.getLabel() + " 2");
        assertEquals(segments.getFirst().color(), UIUtil.getInactiveTextColor(),
                "removed is not a verdict, so it is drawn in the same color as the rest of the bar");
    }

    /**
     * The one bucket with two names. A case nobody reached is pending while the
     * run is open and untested once it is over, which is the run changing it
     * rather than a tester - so the line has to say whichever is true now, or it
     * tells a tester their untouched cases were given up on while they are still
     * working through them.
     */
    @Test
    public void untouchedCasesArePendingUntilTheRunGivesUpOnThem() {
        assertEquals(words(run(0, 0, 0, 7, 0)), TestStatus.PENDING.getLabel() + " 7");

        for (final TestRunStatus over : new TestRunStatus[]{TestRunStatus.COMPLETED, TestRunStatus.CLOSED}) {
            assertEquals(of(run(0, 0, 0, 7, 0), over).getFirst().text(), TestStatus.UNTESTED.getLabel() + " 7",
                    "a run that is " + over.getLabel() + " has stopped waiting for them");
        }
    }

    /**
     * A verdict a tester gives is called the same thing throughout - only the
     * bucket the run owns changes its name.
     */
    @Test
    public void theThreeVerdictsKeepTheirNameWhicheverStateTheRunIsIn() {
        final @NotNull String closed = of(run(1, 1, 1, 0, 0), TestRunStatus.CLOSED).stream()
                .map(ResultAnalysis.Segment::text)
                .collect(Collectors.joining(" · "));

        assertEquals(words(run(1, 1, 1, 0, 0)), closed);
    }

    /**
     * Everything that asks for a bare label is looking at a run that is over -
     * the analysis dialog refuses to open before then - so the plain name stays
     * the finished one.
     */
    @Test
    public void thePlainLabelIsTheFinishedName() {
        assertEquals(ResultAnalysis.UNTESTED.getLabel(), TestStatus.UNTESTED.getLabel());
        assertEquals(ResultAnalysis.UNTESTED.heading(run(0, 0, 0, 7, 0)), TestStatus.UNTESTED.getLabel() + " (7)");
    }
}
