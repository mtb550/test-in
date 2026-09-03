package org.testin.model;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Changing which test cases a run covers, without losing what already happened
 * (#96).
 * <p>
 * A tester who adds a case to a cycle they are part way through must not pay for
 * it with the verdicts already in that cycle, and a case they take out must
 * actually go. Between those two sits the whole risk of the feature: the run is
 * rebuilt from a set of ids, and rebuilding a result from its id rather than
 * carrying the result across would silently blank nine fields per case - the
 * verdict, the actual result, the bug severity and priority, the duration, who
 * ran it and when, and the stack trace.
 * <p>
 * So what is asserted here is mostly that things did <b>not</b> change.
 */
public class RunCoverageTest {

    private static final @NotNull UUID EXECUTED = UUID.fromString("11111111-1111-4111-8111-111111111101");
    private static final @NotNull UUID UNTOUCHED = UUID.fromString("11111111-1111-4111-8111-111111111102");
    private static final @NotNull UUID ADDED = UUID.fromString("11111111-1111-4111-8111-111111111103");

    /**
     * A case that has been run: every field a tester or a framework filled in.
     */
    private static @NotNull TestRunItems executed() {
        return new TestRunItems()
                .setId(EXECUTED)
                .setStatus(TestStatus.FAILED)
                .setActualResult("The lockout counter reset on reload")
                .setBugSeverity(BugSeverity.MAJOR)
                .setBugPriority(BugPriority.HIGH)
                .setDuration(Duration.ofSeconds(252))
                .setExecutedBy("Muteb")
                .setExecutedAt(ZonedDateTime.now())
                .setStacktrace("java.lang.AssertionError: expected locked");
    }

    private static @NotNull TestRunDto aRunOf(final @NotNull TestRunItems... items) {
        return new TestRunDto().setResults(new ArrayList<>(List.of(items)));
    }

    private static @NotNull Set<UUID> ids(final @NotNull UUID... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }

    private static @NotNull List<UUID> coveredBy(final @NotNull TestRunDto run) {
        return run.getResults().stream().map(TestRunItems::getId).collect(Collectors.toList());
    }

    @Test
    public void aCaseThatStaysKeepsEverythingItRecorded() {
        final @NotNull TestRunItems before = executed();
        final @NotNull TestRunDto after = aRunOf(before).coverOnly(ids(EXECUTED, ADDED));

        final @NotNull TestRunItems kept = after.getResults().getFirst();

        assertSame(kept, before,
                "The result is carried across, not rebuilt from its id. Rebuilding it is how all nine fields below"
                        + " would go quiet, one release after somebody 'simplified' this into a stream of new items");
        assertEquals(kept.getStatus(), TestStatus.FAILED);
        assertEquals(kept.getActualResult(), "The lockout counter reset on reload");
        assertEquals(kept.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(kept.getBugPriority(), BugPriority.HIGH);
        assertEquals(kept.getDuration(), Duration.ofSeconds(252));
        assertEquals(kept.getExecutedBy(), "Muteb");
        assertFalse(kept.getStacktrace().isBlank());
    }

    @Test
    public void aCaseThatArrivesIsPending() {
        final @NotNull TestRunDto after = aRunOf(executed()).coverOnly(ids(EXECUTED, ADDED));

        final @NotNull TestRunItems fresh = after.getResults().get(1);

        assertEquals(fresh.getId(), ADDED, "A case added to a test set after the run was created is the whole point");
        assertEquals(fresh.getStatus(), TestStatus.PENDING);
        assertEquals(fresh.getDuration(), Duration.ZERO);
        assertEquals(fresh.getExecutedAt(), Config.NOT_EXECUTED,
                "Nothing has run it, so it carries no plausible execution time");
    }

    @Test
    public void aCaseThatGoesIsGoneWithWhatItRecorded() {
        final @NotNull TestRunDto after = aRunOf(executed(), new TestRunItems().setId(UNTOUCHED)).coverOnly(ids(UNTOUCHED));

        assertEquals(coveredBy(after), List.of(UNTOUCHED),
                "Unticking an executed case discards its result, with no confirmation - decided 2026-09-03."
                        + " The undo on the action is what that decision leans on");
    }

    @Test
    public void theRunKeepsItsOwnOrderAndNewCasesGoOnTheEnd() {
        final @NotNull TestRunDto run = aRunOf(new TestRunItems().setId(UNTOUCHED), executed());

        // Asked for in the opposite order, which is the order the selection tree
        // hands them over in: the run's order is the run's, not the tree's.
        final @NotNull TestRunDto after = run.coverOnly(ids(ADDED, EXECUTED, UNTOUCHED));

        assertEquals(coveredBy(after), List.of(UNTOUCHED, EXECUTED, ADDED),
                "Editing a run is not a re-sort. The cases it already covered stay where they were and new ones append");
    }

    @Test
    public void theRunItWasIsLeftAlone() {
        final @NotNull TestRunDto run = aRunOf(executed(), new TestRunItems().setId(UNTOUCHED));
        final @NotNull TestRunDto after = run.coverOnly(ids(EXECUTED));

        assertNotSame(after, run);
        assertEquals(coveredBy(run), List.of(EXECUTED, UNTOUCHED),
                "The caller still holds what the run was, which is what the undo puts back. Editing the run in place"
                        + " would leave the undo holding the same object it was meant to restore");
        assertEquals(coveredBy(after), List.of(EXECUTED));
    }

    @Test
    public void theConfigurationAndTheTimingsRideAlong() {
        final @NotNull ZonedDateTime started = ZonedDateTime.now().minusHours(2);

        final @NotNull TestRunDto run = aRunOf(executed()).setExecutionStartedAt(started);
        run.getConfiguration().put(TestRunConfiguration.PLATFORM, "Web");
        run.getResultAnalysis().put(ResultAnalysis.FAILED, "The lockout counter is the one to chase");

        final @NotNull TestRunDto after = run.coverOnly(ids(EXECUTED));

        assertEquals(after.getExecutionStartedAt(), started, "Changing what a run covers does not restart it");
        assertEquals(after.getConfiguration().get(TestRunConfiguration.PLATFORM), "Web");
        assertEquals(after.getResultAnalysis().get(ResultAnalysis.FAILED), "The lockout counter is the one to chase",
                "What the tester wrote about the verdicts is a fact about this run and survives a change of scope");
    }

    @Test
    public void aRunCoveringNothingIsPossibleHereAndRefusedByTheDialog() {
        final @NotNull TestRunDto after = aRunOf(executed()).coverOnly(Set.of());

        assertTrue(after.getResults().isEmpty(),
                "The model does what it is asked. Refusing an empty run is the dialog's job - its Save button follows"
                        + " the checked state and goes dead at zero, the same way Create does");
    }
}
