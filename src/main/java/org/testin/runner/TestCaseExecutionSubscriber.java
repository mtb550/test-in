package org.testin.runner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.Failure;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * <p>
 * <b>One of these per project, not one per surface.</b> Each surface used to
 * build its own, and each of those wrote the verdict into the run registry - so
 * the model was updated once per open surface, and not at all when none was
 * open. Running a test set from the tree in a session where no run editor, no
 * test editor and no view panel had been opened stored no verdict at all, and
 * every card afterwards showed no result for a run that had actually passed.
 * <p>
 * Recording is not a side effect of drawing. This records, once, whether or not
 * anything is watching - {@code StartupActivity} builds it - and a surface
 * registers to be told afterwards. Registering rather than subscribing
 * separately is what keeps the order the comment in {@code report} relies on: a
 * surface reads what is running from the runner, so it cannot be told before
 * the runner has been.
 */
@Service(Service.Level.PROJECT)
public final class TestCaseExecutionSubscriber implements Disposable {

    private final @NotNull Project p;

    /**
     * The surfaces to redraw once a report has landed: the run editor, the test
     * editor's list, the view panel. Run on the EDT, because that is where the
     * report is handled, and each is dropped when the surface holding it goes.
     */
    private final @NotNull List<Reported> surfaces = new CopyOnWriteArrayList<>();

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

    TestCaseExecutionSubscriber(final @NotNull Project p) {
        this.p = p;

        p.getMessageBus().connect(this).subscribe(TestCaseExecutionListener.TOPIC,
                (TestCaseExecutionListener) (testName, status, duration, failure) ->
                        // The runner reports from its own thread, and everything
                        // below writes fields a renderer reads - so it happens on
                        // the EDT, and the redraw needs no hop of its own.
                        ApplicationManager.getApplication().invokeLater(
                                () -> record(testName, status, duration, failure)));
    }

    /**
     * Starts recording verdicts for this project, whether or not any surface
     * ever opens. Called from {@code StartupActivity}, which claims its own run
     * once per project.
     */
    public static void initRecording(final @NotNull Project p) {
        Services.getInstance(p, TestCaseExecutionSubscriber.class);
    }

    /**
     * Asks to be told when a report lands, for as long as the surface lives.
     */
    public static void onReported(final @NotNull Project p, final @NotNull Disposable parentDisposable, final @NotNull Reported onUpdated) {
        final @NotNull TestCaseExecutionSubscriber recorder = Services.getInstance(p, TestCaseExecutionSubscriber.class);

        recorder.surfaces.add(onUpdated);
        Disposer.register(parentDisposable, () -> recorder.surfaces.remove(onUpdated));
    }

    private void record(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        Logger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        parseUuid(testName).flatMap(Services.getInstance(p, ProjectIndexer.class)::findTestCase).ifPresentOrElse(
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
        // a moment ago. Guaranteed now rather than hoped for - one subscriber
        // records and then calls the surfaces, where two independent
        // subscriptions would have run in whatever order the bus chose.
        surfaces.forEach(surface -> surface.accept(tc, reportedStatus, duration, failure));
    }

    @Override
    public void dispose() {
        surfaces.clear();
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
