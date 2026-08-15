package org.testin.model;

import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestStatus;
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
                .actualResult("NPE on the login button")
                .stacktrace("java.lang.NullPointerException at Login.click(Login.java:42)")
                .build();
    }

    @Test
    public void passingAFailedCaseClearsEverythingTheFailureDescribed() {
        final TestRunItems item = failedWithBug();

        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getStatus(), TestStatus.PASSED);
        assertEquals(item.getBugSeverity(), BugSeverity.EMPTY);
        assertEquals(item.getBugPriority(), BugPriority.EMPTY);
        assertEquals(item.getActualResult(), "", "the failure text describes a failure that no longer exists");
        assertEquals(item.getStacktrace(), "", "likewise the stacktrace");
    }

    @Test
    public void failingAgainKeepsTheBugTheDialogJustCollected() {
        final TestRunItems item = failedWithBug();

        item.recordVerdict(TestStatus.FAILED, "tester");

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR, "re-failing must not wipe the details");
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
        assertEquals(item.getActualResult(), "NPE on the login button");
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
    public void aCaseBlockedInBetweenStillClearsWhenItFinallyPasses() {
        final TestRunItems item = failedWithBug();

        // The route does not matter, only the destination: details collected
        // while failing are just as stale after a detour through Blocked.
        item.recordVerdict(TestStatus.BLOCKED, "tester");
        item.recordVerdict(TestStatus.PASSED, "tester");

        assertEquals(item.getBugSeverity(), BugSeverity.EMPTY);
        assertEquals(item.getBugPriority(), BugPriority.EMPTY);
        assertEquals(item.getActualResult(), "");
        assertEquals(item.getStacktrace(), "");
    }

    @Test
    public void blockingAFailedCaseKeepsTheDetails() {
        final TestRunItems item = failedWithBug();

        // Blocked is not a pass - the reported bug may still be real, so only
        // passing clears. See the note on recordVerdict.
        item.recordVerdict(TestStatus.BLOCKED, "tester");

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
        assertEquals(item.getActualResult(), "NPE on the login button");
        assertEquals(item.getStacktrace().isEmpty(), false);
    }
}
