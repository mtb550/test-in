/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class ResultAnalysisSegmentsTest {

    private static @NotNull TestRunSummary run(final long passed, final long failed, final long blocked, final long untested, final long removed) {
        return new TestRunSummary(passed + failed + blocked + untested + removed, passed, failed, blocked, untested, removed, "");
    }

    private static @NotNull List<ResultAnalysis.Segment> of(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus run) {
        return ResultAnalysis.segments(summary, run);
    }

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

    @Test
    public void aVerdictNoTestCaseCarriesIsLeftOut() {
        assertEquals(words(run(12, 0, 0, 108, 0)), "Passed 12 · Pending 108");
    }

    @Test
    public void theVerdictsReadInTheOrderTheEnumDeclaresThem() {
        assertEquals(words(run(1, 2, 3, 4, 0)), "Passed 1 · Failed 2 · Blocked 3 · Pending 4");
    }

    @Test
    public void testCasesDeletedUnderTheRunAreCountedToo() {
        assertEquals(words(run(5, 0, 0, 0, 2)), "Passed 5 · Removed 2");
    }

    @Test
    public void theNamesAreTheStatusesOwn() {
        assertEquals(words(run(1, 0, 0, 0, 0)), TestStatus.PASSED.getLabel() + " 1");
    }

    @Test
    public void aVerdictIsPaintedInItsOwnColor() {
        final @NotNull Color painted = of(run(0, 3, 0, 0, 0), TestRunStatus.IN_PROGRESS).getFirst().color();

        assertTrue(painted.getRGB() == Color.decode("#" + ResultAnalysis.FAILED.getHexColor()).getRGB()
                        || painted.getRGB() == Color.decode("#" + ResultAnalysis.FAILED.getDarkHexColor()).getRGB(),
                "failed should be painted red, in whichever of its two reds suits the theme");
    }

    @Test
    public void everyVerdictsColorFollowsTheThemeRatherThanBeingPickedOnce() {
        for (final ResultAnalysis.Segment segment : of(run(1, 1, 1, 1, 0), TestRunStatus.IN_PROGRESS)) {
            assertTrue(segment.color() instanceof JBColor,
                    segment.text() + " is drawn in a color that was resolved once and cannot follow a theme change");
        }
    }

    @Test
    public void aRemovedTestCaseIsCountedWithoutBeingPaintedAsAVerdict() {
        final @NotNull List<ResultAnalysis.Segment> segments = of(run(0, 0, 0, 0, 2), TestRunStatus.CLOSED);

        assertEquals(segments.size(), 1);
        assertEquals(segments.getFirst().text(), TestStatus.REMOVED.getLabel() + " 2");
        assertEquals(segments.getFirst().color(), UIUtil.getInactiveTextColor(),
                "removed is not a verdict, so it is drawn in the same color as the rest of the bar");
    }

    @Test
    public void untouchedTestCasesArePendingUntilTheRunGivesUpOnThem() {
        assertEquals(words(run(0, 0, 0, 7, 0)), TestStatus.PENDING.getLabel() + " 7");

        for (final TestRunStatus over : new TestRunStatus[]{TestRunStatus.COMPLETED, TestRunStatus.CLOSED}) {
            assertEquals(of(run(0, 0, 0, 7, 0), over).getFirst().text(), TestStatus.UNTESTED.getLabel() + " 7",
                    "a run that is " + over.getLabel() + " has stopped waiting for them");
        }
    }

    @Test
    public void theThreeVerdictsKeepTheirNameWhicheverStateTheRunIsIn() {
        final @NotNull String closed = of(run(1, 1, 1, 0, 0), TestRunStatus.CLOSED).stream()
                .map(ResultAnalysis.Segment::text)
                .collect(Collectors.joining(" · "));

        assertEquals(words(run(1, 1, 1, 0, 0)), closed);
    }

    @Test
    public void thePlainLabelIsTheFinishedName() {
        assertEquals(ResultAnalysis.UNTESTED.getLabel(), TestStatus.UNTESTED.getLabel());
        assertEquals(ResultAnalysis.UNTESTED.heading(run(0, 0, 0, 7, 0)), TestStatus.UNTESTED.getLabel() + " (7)");
    }
}
