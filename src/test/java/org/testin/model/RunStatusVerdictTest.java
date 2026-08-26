package org.testin.model;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.*;

/**
 * What a TestNG report writes into a test run.
 * <p>
 * The run editor runs a case from its hover icon and records the result on the
 * case, so something has to say which execution report is a verdict and which is
 * not. That is this enum's to answer, not the editor's - otherwise the two
 * vocabularies get mapped by hand at every surface that needs it.
 */
public class RunStatusVerdictTest {

    @Test
    public void passingAndFailingAreVerdicts() {
        assertEquals(RunStatus.PASSED.getVerdict(), Optional.of(TestStatus.PASSED),
                "a case TestNG passed is a case the run records as passed");
        assertEquals(RunStatus.FAILED.getVerdict(), Optional.of(TestStatus.FAILED),
                "and a failure is a failure");
    }

    @Test
    public void startingIsNotAVerdict() {
        assertTrue(RunStatus.RUNNING.getVerdict().isEmpty(),
                "a case that has started has not finished, so the run records nothing yet");
    }

    @Test
    public void aStoppedCaseIsNotAFailure() {
        assertTrue(RunStatus.IDLE.getVerdict().isEmpty(),
                "nobody found a defect - the case simply did not finish, so its status is left alone (#34)");
    }

    @Test
    public void everyStatusAnswersWithoutANull() {
        for (final RunStatus status : RunStatus.values()) {
            assertNotNull(status.getVerdict(), status + " must answer with a value of its own type");
        }
    }

    @Test
    public void aVerdictIsAlwaysOneATesterCouldHaveGivenByHand() {
        // The automated path goes through the same recordVerdict as the manual
        // one, so anything this returns has to be a status the run editor's own
        // menu offers - otherwise a run could end up holding a verdict no
        // tester could have set or cleared.
        for (final RunStatus status : RunStatus.values()) {
            status.getVerdict().ifPresent(verdict -> assertTrue(
                    verdict == TestStatus.PASSED || verdict == TestStatus.FAILED || verdict == TestStatus.BLOCKED,
                    status + " maps to " + verdict + ", which is not a verdict a tester chooses"));
        }
    }
}
