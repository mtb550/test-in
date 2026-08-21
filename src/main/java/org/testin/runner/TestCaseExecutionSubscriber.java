package org.testin.runner;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Marks the test case a run is reporting on, and tells one screen to redraw.
 * <p>
 * TestNG names the method it is running, and a Testin-generated method is named
 * by the test case's id - so the first report identifies the case outright. The
 * reports that follow name the same method by a name of TestNG's own, which is
 * why the first one is remembered against it: everything after it is matched
 * through that map.
 * <p>
 * There were two of these, one for the test editor's list and one for the view
 * panel, with the same body and the same three fields (#66, finding 20). They
 * differed in what they redrew, which is the argument now.
 */
public final class TestCaseExecutionSubscriber {

    /**
     * TestNG's name for a method, against the test case it turned out to be.
     * Written from the runner's thread and read on the EDT.
     */
    private final @NotNull Map<String, UUID> uuidToDtoId = new ConcurrentHashMap<>();

    private final @NotNull Project p;

    private final @NotNull ProjectIndexer indexer;

    /**
     * What to redraw once a report has landed: the editor's list, or the view
     * panel. Run on the EDT, because that is where the report is handled.
     */
    private final @NotNull Runnable onUpdated;

    /**
     * The case the run is on, empty until one reports itself.
     */
    private volatile @NotNull Optional<UUID> runningDtoId = Optional.empty();

    public TestCaseExecutionSubscriber(final @NotNull Project p, final @NotNull Disposable parentDisposable,
                                       final @NotNull Runnable onUpdated) {
        this.p = p;
        this.indexer = Services.getInstance(p, ProjectIndexer.class);
        this.onUpdated = onUpdated;

        p.getMessageBus().connect(parentDisposable).subscribe(TestCaseExecutionListener.TOPIC,
                (TestCaseExecutionListener) (testName, status, error) ->
                        // The runner reports from its own thread, and everything
                        // below writes fields a renderer reads - so it happens on
                        // the EDT, and the redraw needs no hop of its own.
                        ApplicationManager.getApplication().invokeLater(
                                () -> record(testName, status, error)));
    }

    private void record(final @NotNull String testName, final @NotNull RunStatus status, final @NotNull String error) {
        Logger.debug("Execution report: testName='" + testName + "', status='" + status + "'");

        final @NotNull Optional<TestCaseDto> byId = parseUuid(testName).flatMap(indexer::findTestCase);
        byId.ifPresent(tc -> runningDtoId = Optional.of(tc.getId()));

        final @NotNull Optional<TestCaseDto> reported = byId.isPresent()
                ? byId
                : Optional.ofNullable(uuidToDtoId.get(testName)).flatMap(indexer::findTestCase);

        reported.ifPresent(tc -> {
            final @NotNull RunStatus reportedStatus = verdictFor(tc, status);

            Logger.debug("  reporting on '" + tc.getDescription() + "', tempStatus='" + reportedStatus + "'");
            tc.setTempStatus(reportedStatus);
            tc.setTempError(error);
        });

        // The first report TestNG makes under a name of its own: it is about the
        // case that was already running, so the name is remembered against it.
        if (reported.isEmpty() && status == RunStatus.RUNNING && !uuidToDtoId.containsKey(testName)) {
            runningDtoId.flatMap(indexer::findTestCase).ifPresent(tc -> {
                Logger.debug("  mapping '" + testName + "' to '" + tc.getDescription() + "'");
                uuidToDtoId.put(testName, tc.getId());
            });
        }

        if (reported.isPresent()) onUpdated.run();
    }

    /**
     * What the report means for this case.
     * <p>
     * A case the tester stopped reports itself finished without having passed,
     * which reads exactly like a failure. It is not one: nobody found a defect,
     * the case simply did not finish (#34). Decided here because this is the one
     * place that knows which case a report is about - the runner sees only the
     * name TestNG chose for the method.
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
