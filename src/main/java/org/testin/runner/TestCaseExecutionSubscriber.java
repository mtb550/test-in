package org.testin.runner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Marks the test case a run is reporting on, and tells one screen to redraw.
 * <p>
 * A Testin-generated method is named by its test case's id, so the name in the
 * report is the answer: there is nothing to look up and nothing to remember
 * between reports.
 * <p>
 * It used to remember more. The premise was that the platform reports a method
 * once under the generated name and again under a name of its own, so the first
 * report was recorded against that second name and everything after it was
 * matched through the map. The log says otherwise - 2,827 reports, every one of
 * them a bare id, and the map never written to once. What the map did instead
 * was hold a fallback that attributed an unrecognized name to whichever case
 * reported last, which was harmless while one case ran per process and became a
 * way to mark an arbitrary case failed once fifty of them shared one (#66).
 * <p>
 * So an unrecognized name is now nobody's. That is the honest answer for a
 * configuration method someone adds to a generated class, and for a framework
 * whose naming this does not know yet - and it is logged, so the second case
 * shows up as a line to read rather than as a verdict on the wrong case.
 * <p>
 * There were two of these, one for the test editor's list and one for the view
 * panel, with the same body and the same three fields (#66, finding 20). They
 * differed in what they redrew, which is the argument now.
 */
public final class TestCaseExecutionSubscriber {

    private final @NotNull Project p;

    private final @NotNull ProjectIndexer indexer;

    /**
     * What to redraw once a report has landed: the editor's list, or the view
     * panel. Run on the EDT, because that is where the report is handled.
     */
    private final @NotNull Reported onUpdated;

    /**
     * What a surface is told when a report lands: which case, and what it said.
     * <p>
     * A bare Runnable before, because both listeners only repainted. The run
     * editor needs more than "something changed" - it writes the verdict into
     * the run, times the case, and records what the framework said when one did
     * not pass - and having it re-derive that from the broadcast would put the
     * name matching and the stopped-is-not-failed rule in a second place.
     */
    @FunctionalInterface
    public interface Reported {
        void accept(final @NotNull TestCaseDto tc, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure);
    }

    public TestCaseExecutionSubscriber(final @NotNull Project p, final @NotNull Disposable parentDisposable, final @NotNull Reported onUpdated) {
        this.p = p;
        this.indexer = Services.getInstance(p, ProjectIndexer.class);
        this.onUpdated = onUpdated;

        p.getMessageBus().connect(parentDisposable).subscribe(TestCaseExecutionListener.TOPIC,
                (TestCaseExecutionListener) (testName, status, duration, failure) ->
                        // The runner reports from its own thread, and everything
                        // below writes fields a renderer reads - so it happens on
                        // the EDT, and the redraw needs no hop of its own.
                        ApplicationManager.getApplication().invokeLater(
                                () -> record(testName, status, duration, failure)));
    }

    private void record(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        Logger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        parseUuid(testName).flatMap(indexer::findTestCase).ifPresentOrElse(
                tc -> report(tc, status, duration, failure),
                () -> Logger.debug("  '" + testName + "' is not a generated test case - reported against none"));
    }

    private void report(final @NotNull TestCaseDto tc, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        final @NotNull RunStatus reportedStatus = verdictFor(tc, status);

        Logger.debug("  reporting on '" + tc.getDescription() + "': " + reportedStatus + " " + failure.message());

        // Recorded against the case's id rather than on the instance in hand.
        // This one is replaced by the next rescan, and a verdict that lived on
        // it went with it - which is how a case that had just passed lost its
        // badge at the tester's next keystroke (#116).
        Services.getInstance(p, TestNGExecution.class).reported(tc.getId(), reportedStatus);

        // After the runner has been told, not before: a surface asked to redraw
        // reads what is running from there, and would paint the state as it was
        // a moment ago.
        onUpdated.accept(tc, reportedStatus, duration, failure);
    }

    /**
     * What the report means for this case.
     * <p>
     * A case the tester stopped reports itself finished without having passed,
     * which reads exactly like a failure. It is not one: nobody found a defect,
     * the case simply did not finish (#34). Decided here because this is the one
     * place that knows which case a report is about - the runner sees only the
     * name the framework chose for the method.
     */
    private @NotNull RunStatus verdictFor(final @NotNull TestCaseDto tc, final @NotNull RunStatus status) {
        final boolean stopped = status == RunStatus.FAILED
                && Services.getInstance(p, TestNGExecution.class).isStopped(tc);

        return stopped ? RunStatus.IDLE : status;
    }

    /**
     * The test name as an id, when that is what it is: only a Testin-generated
     * method is named by one.
     */
    private @NotNull Optional<UUID> parseUuid(final @NotNull String s) {
        try {
            return Optional.of(UUID.fromString(s));
        } catch (final IllegalArgumentException notAnId) {
            return Optional.empty();
        }
    }
}
