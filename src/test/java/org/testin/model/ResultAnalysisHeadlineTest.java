package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The one line the run editor's status bar shows about how a run is going.
 * <p>
 * Asserted here rather than through the bar because the rule is not a Swing one:
 * which verdicts appear, in which order, under which names, and which of them
 * carry a color. The bar only sets the text it is handed.
 */
public class ResultAnalysisHeadlineTest {

    private static @NotNull TestRunSummary run(final long passed, final long failed, final long blocked, final long untested, final long removed) {
        return new TestRunSummary(passed + failed + blocked + untested + removed, passed, failed, blocked, untested, removed, 0, "");
    }

    /**
     * What a tester reads, with the markup taken back off. The colors are
     * asserted separately, so the sentence assertions stay legible.
     */
    private static @NotNull String words(final @NotNull TestRunSummary summary) {
        return ResultAnalysis.headline(summary, TestRunStatus.IN_PROGRESS).replaceAll("<[^>]*>", "");
    }

    @Test
    public void aRunNobodyHasTouchedSaysNothing() {
        assertEquals(ResultAnalysis.headline(TestRunSummary.EMPTY, TestRunStatus.IN_PROGRESS), "",
                "empty rather than an empty html document: the bar shows a run with nothing recorded the way it shows a zero duration");
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
     * One of the two colors the verdict declares - which one depends on the
     * theme the test happens to run under, and that is the point: the line is
     * built when it is shown, not baked in.
     */
    @Test
    public void aVerdictIsPaintedInItsOwnColor() {
        final @NotNull String html = ResultAnalysis.headline(run(0, 3, 0, 0, 0), TestRunStatus.IN_PROGRESS);

        assertTrue(html.contains("#" + ResultAnalysis.FAILED.getHexColor()) || html.contains("#" + ResultAnalysis.FAILED.getDarkHexColor()),
                "failed should be painted red, in whichever of its two reds suits the theme: " + html);
    }

    /**
     * A case deleted out from under the run is not a verdict anybody reached, so
     * it is counted without being painted like one.
     */
    @Test
    public void aRemovedCaseIsCountedWithoutBeingPaintedAsAVerdict() {
        final @NotNull String html = ResultAnalysis.headline(run(0, 0, 0, 0, 2), TestRunStatus.CLOSED);

        assertEquals(html, "<html><nobr>" + TestStatus.REMOVED.getLabel() + " 2</nobr></html>");
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
            assertEquals(ResultAnalysis.headline(run(0, 0, 0, 7, 0), over).replaceAll("<[^>]*>", ""),
                    TestStatus.UNTESTED.getLabel() + " 7",
                    "a run that is " + over.getLabel() + " has stopped waiting for them");
        }
    }

    /**
     * A verdict a tester gives is called the same thing throughout - only the
     * bucket the run owns changes its name.
     */
    @Test
    public void theThreeVerdictsKeepTheirNameWhicheverStateTheRunIsIn() {
        assertEquals(words(run(1, 1, 1, 0, 0)),
                ResultAnalysis.headline(run(1, 1, 1, 0, 0), TestRunStatus.CLOSED).replaceAll("<[^>]*>", ""));
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
