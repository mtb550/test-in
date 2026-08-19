package org.testin.model;

import org.testin.model.dto.TestRunDto;
import org.testin.util.Display;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * "Not executed" is an empty timestamp of the same type, never a null and never
 * "now": a case nobody gave a verdict and a run nobody started show blank
 * wherever their timestamps appear, and one helper decides what blank is.
 */
public class NotExecutedTimestampTest {

    @Test
    public void aFreshRunItemHasNoExecutionTime() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        assertTrue(Config.isNotExecuted(item.getExecutedAt()));
        assertEquals(Display.formatDate(item.getExecutedAt()), "");
    }

    @Test
    public void aVerdictGivesTheCaseARealTime() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertFalse(Config.isNotExecuted(item.getExecutedAt()));
        assertFalse(Display.formatDate(item.getExecutedAt()).isEmpty());
    }

    @Test
    public void theEpochIsStillEmptyAfterTheMapperMovesItIntoAnotherZone() {
        // The mapper adjusts every timestamp it reads to the system zone, so the
        // epoch comes back from a run file as 03:00 in Riyadh, not as 00:00 UTC.
        final ZonedDateTime readBack = Config.NOT_EXECUTED.withZoneSameInstant(ZoneId.of("Asia/Riyadh"));

        assertTrue(Config.isNotExecuted(readBack));
        assertEquals(Display.formatDate(readBack), "");
    }

    @Test
    public void aFreshRunHasNeitherStartedNorEnded() {
        final TestRunDto run = new TestRunDto();

        assertEquals(Display.formatDate(run.getExecutionStartedAt()), "");
        assertEquals(Display.formatDate(run.getExecutionEndedAt()), "");
    }

    @Test
    public void aRunThatNeverStartedHasNoEndToStamp() {
        final TestRunDto run = new TestRunDto();

        // Completed from the tree without ever pressing Start.
        run.markExecutionEnded();

        assertEquals(Display.formatDate(run.getExecutionEndedAt()), "");
    }

    @Test
    public void theFirstStartIsKeptAndTheLastEndWins() throws InterruptedException {
        final TestRunDto run = new TestRunDto();

        run.markExecutionStarted();
        final ZonedDateTime firstStart = run.getExecutionStartedAt();
        run.markExecutionEnded();
        final ZonedDateTime firstEnd = run.getExecutionEndedAt();

        // Both stamps are truncated to the second, so a second must pass for the
        // difference to be observable at all.
        Thread.sleep(1100);
        run.markExecutionStarted();
        run.markExecutionEnded();

        assertEquals(run.getExecutionStartedAt(), firstStart, "a resumed run still started when it started");
        assertTrue(run.getExecutionEndedAt().isAfter(firstEnd), "the run ended when it last stopped");
    }

    /**
     * Runs built before the empty default carry a creation-time stamp on every
     * case, including the ones nobody ran. Reading such a run drops those, and
     * only those: a real verdict's time is not a default and must survive.
     */
    @Test
    public void readingAnOldRunDropsTheStampsNoVerdictEarned() {
        final ZonedDateTime asIfCreated = ZonedDateTime.now().minusDays(30);

        final TestRunItems pending = TestRunItems.builder().id(UUID.randomUUID())
                .status(TestStatus.PENDING).executedAt(asIfCreated).build();
        final TestRunItems untested = TestRunItems.builder().id(UUID.randomUUID())
                .status(TestStatus.UNTESTED).executedAt(asIfCreated).build();
        final TestRunItems passed = TestRunItems.builder().id(UUID.randomUUID())
                .status(TestStatus.PASSED).executedAt(asIfCreated).build();

        final TestRunDto run = new TestRunDto();
        run.setResults(List.of(pending, untested, passed));

        run.dropStampsWithoutVerdict();

        assertEquals(Display.formatDate(pending.getExecutedAt()), "", "queued, never executed");
        assertEquals(Display.formatDate(untested.getExecutedAt()), "", "the run ended without reaching it");
        assertEquals(passed.getExecutedAt(), asIfCreated, "a verdict's own time is not a default");
    }
}
