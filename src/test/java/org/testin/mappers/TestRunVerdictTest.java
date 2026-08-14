package org.testin.mappers;

import org.testin.enums.BugPriority;
import org.testin.enums.BugSeverity;
import org.testin.enums.TestStatus;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * Recording a verdict on a run item.
 * <p>
 * Bug severity and priority are only ever collected by the failure dialog, so a
 * case that goes from failing to passing must not keep them: they would survive
 * into the run JSON and into every report generated from it, describing a bug
 * nobody is reporting any more.
 */
public class TestRunVerdictTest {

    private static TestRunItems failedWithBug() {
        return TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(TestStatus.FAILED)
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .build();
    }

    @Test
    public void passingAFailedCaseClearsTheBug() {
        final TestRunItems item = failedWithBug();

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getStatus(), TestStatus.PASSED);
        assertEquals(item.getBugSeverity(), BugSeverity.EMPTY);
        assertEquals(item.getBugPriority(), BugPriority.EMPTY);
    }

    @Test
    public void failingAgainKeepsTheBugTheDialogJustCollected() {
        final TestRunItems item = failedWithBug();

        item.recordVerdict(TestStatus.FAILED, "tester");

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR, "re-failing must not wipe the details");
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
    }

    @Test
    public void passingACaseThatNeverFailedChangesNothingElse() {
        final TestRunItems item = TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(TestStatus.PENDING)
                .build();

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getStatus(), TestStatus.PASSED);
        assertEquals(item.getBugSeverity(), BugSeverity.EMPTY);
        assertEquals(item.getBugPriority(), BugPriority.EMPTY);
    }

    @Test
    public void everyVerdictRecordsWhoAndWhen() {
        final TestRunItems item = failedWithBug();

        item.recordVerdict(TestStatus.BLOCKED, "muteb");

        assertEquals(item.getExecutedBy(), "muteb");
        assertEquals(item.getExecutedAt().getNano(), 0, "stamped to the second, as the run JSON stores it");
    }

    @Test
    public void blockingAFailedCaseKeepsTheBugForNow() {
        final TestRunItems item = failedWithBug();

        // Only FAILED -> PASSED clears today. Blocked is not a pass, so the bug
        // that was reported may still be real - see the note on recordVerdict.
        item.recordVerdict(TestStatus.BLOCKED, "tester");

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
    }
}
