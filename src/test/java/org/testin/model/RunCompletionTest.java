package org.testin.model;

import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * When a run is over.
 * <p>
 * It used to be the manual walk's answer alone: the walk ran off the end of the
 * list and called the run finished. A tester who executed the whole run through
 * automation watched every card fill in and then found the run still In
 * Progress, with Start Execution offering to begin something that had already
 * happened - because no walk had run off any end.
 * <p>
 * So the question moved to the run, where it does not depend on which of the two
 * executed it.
 */
public class RunCompletionTest {

    private static TestRunItems item(final TestStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    private static TestRunDto runOf(final TestRunItems... items) {
        return TestRunDto.builder().results(List.of(items)).build();
    }

    @Test
    public void everyCaseJudgedMeansTheRunIsOver() {
        assertTrue(runOf(item(TestStatus.PASSED), item(TestStatus.FAILED), item(TestStatus.BLOCKED)).isFullyJudged());
    }

    @Test
    public void oneCaseStillPendingKeepsItOpen() {
        assertFalse(runOf(item(TestStatus.PASSED), item(TestStatus.PENDING)).isFullyJudged(),
                "the run is still expecting something about that case");
    }

    @Test
    public void soDoesOneUntested() {
        assertFalse(runOf(item(TestStatus.PASSED), item(TestStatus.UNTESTED)).isFullyJudged());
    }

    /**
     * The case that would otherwise hold a run open forever: it was deleted from
     * the test set, the run keeps what it recorded about it, and it can never be
     * run again.
     */
    @Test
    public void aDeletedCaseDoesNotHoldItOpenForever() {
        assertTrue(runOf(item(TestStatus.PASSED), item(TestStatus.REMOVED)).isFullyJudged());
    }

    @Test
    public void aRunWithNoCasesIsEmptyRatherThanFinished() {
        assertFalse(runOf().isFullyJudged(),
                "completing a run the moment it is created is the wrong answer to a question nobody asked");
    }
}
