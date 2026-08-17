package org.testin.model;

import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
        assertEquals(Config.formatOrBlank(item.getExecutedAt()), "");
    }

    @Test
    public void aVerdictGivesTheCaseARealTime() {
        final TestRunItems item = TestRunItems.builder().id(UUID.randomUUID()).build();

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertFalse(Config.isNotExecuted(item.getExecutedAt()));
        assertFalse(Config.formatOrBlank(item.getExecutedAt()).isEmpty());
    }

    @Test
    public void theEpochIsStillEmptyAfterTheMapperMovesItIntoAnotherZone() {
        // The mapper adjusts every timestamp it reads to the system zone, so the
        // epoch comes back from a run file as 03:00 in Riyadh, not as 00:00 UTC.
        final ZonedDateTime readBack = Config.NOT_EXECUTED.withZoneSameInstant(ZoneId.of("Asia/Riyadh"));

        assertTrue(Config.isNotExecuted(readBack));
        assertEquals(Config.formatOrBlank(readBack), "");
    }

    @Test
    public void aFreshRunHasNeitherStartedNorEnded() {
        final TestRunDto run = new TestRunDto();

        assertEquals(Config.formatOrBlank(run.getExecutionStartedAt()), "");
        assertEquals(Config.formatOrBlank(run.getExecutionEndedAt()), "");
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
}
