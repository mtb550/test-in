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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The TestNG runs this plugin started: whether one is going, and how to stop it.
 * <p>
 * Both runners used to call {@code ProgramRunnerUtil.executeConfiguration}
 * directly, which is fire-and-forget - nothing was kept, so nothing could be
 * stopped, and there was no stop mechanism anywhere for a button to call (#34).
 * <p>
 * Two things have to stop, and killing only one of them is not a stop. The
 * <b>process</b> is killed rather than asked to end - see {@link #kill}. The
 * <b>launches that have not happened yet</b> are dropped: running a page of
 * twelve schedules twelve launches, each hopping to a pooled thread and back to
 * the EDT, so most are still queued when the tester presses Stop.
 */
@Service(Service.Level.PROJECT)
public final class TestNGExecution {

    private final @NotNull Project p;

    /**
     * The configurations this plugin launched, held so a stop kills what the
     * tester started here and not the build they left going in another tab.
     * <p>
     * By name, which is how the plugin identifies a configuration everywhere
     * else - it looks one up by name and reuses it. What the platform hands back
     * beside a running descriptor is not guaranteed to be the instance that was
     * executed, so matching on the instance alone would miss.
     */
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    /**
     * Which run the tester is on. A stop moves it on, and a launch prepared
     * before that carries the old one and is dropped when its turn comes.
     */
    private final @NotNull AtomicInteger generation = new AtomicInteger();

    /**
     * The cases this plugin was asked to run, so a stop can put back the ones
     * that will never report.
     * <p>
     * A case whose process is killed reports itself finished and is put back by
     * that report. A case whose launch was still queued - most of a page of
     * twelve - reports nothing at all, and without this would keep its Running
     * badge and its stop icon for the rest of the session.
     */
    private final @NotNull Set<TestCaseDto> started = ConcurrentHashMap.newKeySet();

    /**
     * Whether the tester stopped the run the reports still arriving belong to.
     * Cleared by the next case that starts.
     */
    private volatile boolean stopped;

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
        stopped = false;
        started.add(tc);

        TestCaseExecutionListener.broadcast(p, key(tc), RunStatus.RUNNING, "");
    }

    /**
     * The run a launch about to be prepared belongs to. Read where the tester's
     * gesture arrives, not where the launch finally happens - everything in
     * between is asynchronous.
     */
    public int generation() {
        return generation.get();
    }

    /**
     * Starts the configuration, unless the tester has stopped since this launch
     * was asked for.
     */
    public void launch(final int generation, final @NotNull RunnerAndConfigurationSettings settings) {
        if (generation != this.generation.get()) {
            Logger.info("Not starting " + settings.getName() + ": the execution it belonged to was stopped");
            return;
        }

        launchedNames.add(settings.getName());

        Logger.info("Starting " + settings.getName());
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    /**
     * Whether the reports still arriving belong to a run the tester stopped.
     * <p>
     * A killed process reports its test as finished without having passed, which
     * reads exactly like a failure. It is not one: nobody found a defect, the
     * case simply did not finish.
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Kills what is running and drops what has not started yet.
     */
    public void stop() {
        stopped = true;
        generation.incrementAndGet();

        final List<RunContentDescriptor> ours = running();
        Logger.info("Execution stopping: " + ours.size() + " running test process(es) of ours");

        ours.forEach(this::kill);

        // Every case still showing as running is one no report will arrive for:
        // its launch never happened, or its process died before TestNG could say
        // anything about it. A case that already passed or failed keeps that.
        started.stream()
                .filter(tc -> tc.getTempStatus() == RunStatus.RUNNING)
                .forEach(tc -> TestCaseExecutionListener.broadcast(p, key(tc), RunStatus.IDLE, ""));

        started.clear();
        launchedNames.clear();
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
        final ProcessHandler handler = descriptor.getProcessHandler();

        // The platform's null, answered where it arrives. A descriptor with no
        // handler has nothing behind it to stop, and saying so is the difference
        // between a stop that could not act and one that silently did nothing.
        if (handler == null) {
            Logger.warn("Not stopping '" + descriptor.getDisplayName() + "': it has no process handler");
            return;
        }

        handler.putUserData(ProcessHandler.TERMINATION_REQUESTED, Boolean.TRUE);

        if (handler instanceof KillableProcess killable && killable.canKillProcess()) {
            Logger.info("Killing '" + descriptor.getDisplayName() + "'");
            killable.killProcess();

        } else {
            Logger.info("Destroying '" + descriptor.getDisplayName() + "', which cannot be killed");
            handler.destroyProcess();
        }
    }

    private @NotNull List<RunContentDescriptor> running() {
        return ExecutionManager.getInstance(p).getRunningDescriptors(
                settings -> launchedNames.contains(settings.getName()));
    }

    /**
     * How a test case is named in an execution report: a Testin-generated method
     * is named by the case's id, which is what lets the first report identify the
     * case outright.
     */
    private static @NotNull String key(final @NotNull TestCaseDto tc) {
        return tc.getId().toString().toLowerCase();
    }
}
