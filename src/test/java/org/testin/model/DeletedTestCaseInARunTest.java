package org.testin.model;

import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * A test run outlives the test cases it was created from, and a case deleted
 * after the run still has a row in it (#71).
 * <p>
 * The row is drawn from what the run recorded, and takes nothing new: a verdict
 * means "we ran it and this is what happened", and a case that is gone cannot be
 * run again. Three things have to hold together for that, and each of them used
 * to be somewhere else's business:
 * <ul>
 *   <li>the item says it is removed, by name, so the verdict path, the details
 *       editor and the execution walker all ask one question;</li>
 *   <li>the placeholder case keeps the id, because it is the only identity left -
 *       two deleted cases in one run are otherwise the same row twice;</li>
 *   <li>the repair that clears execution stamps without a verdict leaves a
 *       removed row alone, because that case really was executed before it was
 *       deleted.</li>
 * </ul>
 */
public class DeletedTestCaseInARunTest {

    private static TestRunItems removedItem(final UUID id, final ZonedDateTime executedAt) {
        return TestRunItems.builder()
                .id(id)
                .status(TestStatus.REMOVED)
                .executedAt(executedAt)
                .tc(TestCaseDto.deleted(id))
                .build();
    }

    @Test
    public void aRowWhoseCaseIsGoneSaysSoByName() {
        final UUID id = UUID.randomUUID();

        assertTrue(removedItem(id, Config.NOT_EXECUTED).isRemoved(),
                "the verdict path, the details editor and the walker all ask this");
    }

    @Test
    public void aRemovedRowIsNotAVerdict() {
        // REMOVED is what happened to the case, not what a tester decided about
        // it - so it must not be counted as one of the three verdicts.
        assertFalse(TestStatus.REMOVED.isVerdict(), "nobody chose REMOVED from the menu");
    }

    @Test
    public void theRowStillShowsAndStillNamesItsCase() {
        final UUID id = UUID.randomUUID();
        final TestRunItems item = removedItem(id, Config.NOT_EXECUTED);

        final TestCaseDto shown = item.testCase()
                .orElseThrow(() -> new AssertionError("the row is drawn from a placeholder, so there is one"));

        assertEquals(shown.getId(), id, "the id is the only identity a deleted case has left");
        assertTrue(shown.getDescription().contains(id.toString()),
                "the row names itself rather than drawing blank: " + shown.getDescription());
    }

    @Test
    public void theExecutionStampOfARemovedRowSurvivesTheRepair() {
        final ZonedDateTime executed = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        final UUID id = UUID.randomUUID();

        final TestRunDto run = TestRunDto.builder()
                .results(List.of(removedItem(id, executed)))
                .build();

        run.dropStampsWithoutVerdict();

        assertEquals(run.getResults().getFirst().getExecutedAt(), executed,
                "the case was executed before it was deleted, so the time is real");
    }

    @Test
    public void aPendingRowWithoutAVerdictStillLosesItsStamp() {
        // The other half of the same repair, so the exemption above is proven to
        // be an exemption rather than the rule.
        final TestRunItems pending = TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(TestStatus.PENDING)
                .executedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();

        final TestRunDto run = TestRunDto.builder().results(List.of(pending)).build();

        run.dropStampsWithoutVerdict();

        assertTrue(Config.isNotExecuted(run.getResults().getFirst().getExecutedAt()),
                "a row nobody ran carries no execution time");
    }
}
