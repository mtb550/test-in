package org.testin.runner;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.KillableProcess;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The TestNG runs this plugin started: starting them and stopping them.
 * <p>
 * The runner used to call {@code ProgramRunnerUtil.executeConfiguration}
 * directly, which is fire-and-forget - nothing was kept, so nothing could be
 * stopped, and no button had anything to call (#34).
 * <p>
 * <b>A stop reaches the run a case belongs to, and no other.</b> A run holds one
 * case when it was started from a card and all of them when it was started from
 * a test set, because one configuration is one process either way. Stopping a
 * case in a run of twelve therefore stops all twelve - there is one process
 * behind them - and every one of them is put back. Stopping a case that is a run
 * of its own leaves the others alone.
 * <p>
 * Two things have to stop, and killing one of them is not a stop. The
 * <b>process</b> is killed rather than asked to end - see {@link #kill}. The
 * <b>launch that has not happened yet</b> is dropped: a run hops to a pooled
 * thread and back to the EDT before its process exists, and a tester who presses
 * Stop in that second means it.
 * <p>
 * What is running and what each case last did is {@link RunRegistry}'s, which
 * knows nothing of the platform. This class is the half that does: it launches,
 * it kills, and it tells the editors what changed. Every question a surface asks
 * goes to the registry through here.
 */
@Service(Service.Level.PROJECT)
public final class TestNGExecution {

    private final @NotNull Project p;

    /**
     * What is running and what each case last did.
     */
    private final @NotNull RunRegistry registry = new RunRegistry();

    /**
     * Written out rather than generated: a project service's constructor is its
     * contract with the platform, which looks for exactly (Project) and refuses
     * to build the service otherwise. A generated one changes shape whenever a
     * field is added, and the platform only says so at runtime.
     */
    public TestNGExecution(final @NotNull Project p) {
        this.p = p;
    }

    /**
     * A case is on its way to the runner: remembered, and shown as running.
     * <p>
     * Remembering it and marking it are one fact, so they happen together. The
     * platform's own report arrives a second or two later under a name of
     * TestNG's choosing; this is what makes the card change the moment it is
     * clicked.
     */
    public void starting(final @NotNull TestCaseDto tc) {
        registry.starting(tc.getId());

        TestCaseExecutionListener.broadcast(p, key(tc.getId()), RunStatus.RUNNING, "");
    }

    /**
     * Which of these cases the tester still wants run, taking them out of the
     * queue as it answers.
     * <p>
     * Asked immediately before the configuration is built, so a case stopped
     * while the run was being prepared is left out of it rather than started and
     * then killed.
     */
    public @NotNull List<TestCaseDto> stillWanted(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().filter(tc -> registry.take(tc.getId())).toList();
    }

    /**
     * Starts the configuration, and remembers which cases are running under it.
     */
    public void launch(final @NotNull List<TestCaseDto> cases, final @NotNull RunnerAndConfigurationSettings settings) {
        registry.launched(cases.stream().map(TestCaseDto::getId).toList(), settings.getName());

        Logger.info("Starting " + settings.getName() + " with " + cases.size() + " test case(s)");
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    /**
     * A case that was asked for is not going to run. Drops it from the queue and
     * puts the card back, saying nothing - for the paths that have already told
     * the tester why in their own words.
     */
    public void notStarting(final @NotNull TestCaseDto tc) {
        registry.notStarting(tc.getId());

        TestCaseExecutionListener.broadcast(p, key(tc.getId()), RunStatus.IDLE, "");
    }

    /**
     * A case that was asked for has no generated method to run.
     * <p>
     * The card is already showing Running by the time this is reached - it is
     * marked at the click, a second before the launch - so a path that returned
     * quietly left the case looking like it was running for the rest of the
     * session.
     */
    public void noGeneratedCode(final @NotNull TestCaseDto tc) {
        Logger.warn("Not running '" + tc.getDescription() + "': it has no generated code");
        notStarting(tc);

        Services.getInstance(p, Notifier.class).softShowNoGeneratedCode(p, tc.getDescription());
    }

    /**
     * Whether a report arriving for this case belongs to a run the tester
     * stopped.
     */
    public boolean isStopped(final @NotNull TestCaseDto tc) {
        return registry.isStopped(tc.getId());
    }

    /**
     * Whether a stop has something to reach for this case: a launch on its way
     * or a process it is running under.
     * <p>
     * The one answer to "is this case running", for the run/stop slot, the
     * double-run guard and the stop itself. They each used to work it out from a
     * field on the DTO, and all three went blind together the moment a rescan
     * handed the editors fresh instances (#116).
     */
    public boolean isRunning(final @NotNull UUID id) {
        return registry.isRunning(id);
    }

    /**
     * The status a surface paints for this case. Asked of the runner because the
     * answer has to outlive a rescan: both the Running badge of a case mid-run
     * and the verdict of one that has finished used to be dropped along with the
     * DTO instance that carried them.
     */
    public @NotNull RunStatus statusOf(final @NotNull TestCaseDto tc) {
        return registry.statusOf(tc.getId());
    }

    /**
     * A report landed with this case's result: what every surface paints from
     * now on, and the end of the case counting as running.
     */
    void reported(final @NotNull UUID id, final @NotNull RunStatus status) {
        registry.reported(id, status);
    }

    /**
     * Kills the runs these cases belong to and drops their launches that have not
     * started yet.
     * <p>
     * Silent: every path here has a tester watching the card they clicked, and
     * the card changing is the answer.
     *
     * @return how many cases were put back, which is more than were asked for when
     *         they share a run - the count the one notification reports
     */
    public int stop(final @NotNull List<TestCaseDto> cases) {
        final @NotNull RunRegistry.Stop stop = registry.stopping(cases.stream().map(TestCaseDto::getId).toList());
        if (stop.cases().isEmpty()) return 0;

        final @NotNull List<RunContentDescriptor> theirs = running(stop.runs());
        Logger.info("Stopping " + stop.cases().size() + " test case(s) in " + stop.runs().size()
                + " run(s): " + theirs.size() + " had reached a process");

        theirs.forEach(this::kill);
        stop.cases().forEach(id -> TestCaseExecutionListener.broadcast(p, key(id), RunStatus.IDLE, ""));

        return stop.cases().size();
    }

    /**
     * Ends one test process the way the IDE's own Stop button ends a stubborn
     * one: it requests termination, and a handler that can kill its process tree
     * asks for the kill rather than for the JVM's cooperation.
     * <p>
     * The polite ask alone is what the platform tries first, and it was not
     * enough here - a run stopped two seconds in still reported itself passed
     * fourteen seconds later, having run to the end (#34).
     * <p>
     * Says which of the two it did, because the ways this can fail look
     * identical from the outside: a descriptor whose process is already gone,
     * and a live process that ignores the request.
     */
    private void kill(final @NotNull RunContentDescriptor descriptor) {
        // Answered where it arrives. A descriptor with no handler has nothing
        // behind it to stop, and saying so is the difference between a stop that
        // could not act and one that silently did nothing.
        final @NotNull Optional<ProcessHandler> found = Optional.ofNullable(descriptor.getProcessHandler());
        if (found.isEmpty()) {
            Logger.warn("Not stopping '" + descriptor.getDisplayName() + "': it has no process handler");
            return;
        }

        final @NotNull ProcessHandler handler = found.orElseThrow();

        handler.putUserData(ProcessHandler.TERMINATION_REQUESTED, Boolean.TRUE);

        if (handler instanceof KillableProcess killable && killable.canKillProcess()) {
            Logger.info("Killing '" + descriptor.getDisplayName() + "'");
            killable.killProcess();

        } else {
            Logger.info("Destroying '" + descriptor.getDisplayName() + "', which cannot be killed");
            handler.destroyProcess();
        }
    }

    /**
     * The running processes started by these configurations, and only ones this
     * plugin launched - a tester's own configuration of the same name is theirs
     * to stop.
     */
    private @NotNull List<RunContentDescriptor> running(final @NotNull Set<String> names) {
        return ExecutionManager.getInstance(p).getRunningDescriptors(
                settings -> names.contains(settings.getName()) && registry.launchedHere(settings.getName()));
    }

    /**
     * How a test case is named in an execution report: a Testin-generated method
     * is named by the case's id, which is what lets the first report identify the
     * case outright.
     */
    private static @NotNull String key(final @NotNull UUID id) {
        return id.toString().toLowerCase();
    }
}
