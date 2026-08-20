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
import org.testin.codegen.Fqcn;
import org.testin.logger.Logger;
import org.testin.model.RunStatus;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The TestNG runs this plugin started: which case runs under which
 * configuration, and how to stop the ones the tester asked to stop.
 * <p>
 * Both runners used to call {@code ProgramRunnerUtil.executeConfiguration}
 * directly, which is fire-and-forget - nothing was kept, so nothing could be
 * stopped, and no button had anything to call (#34).
 * <p>
 * <b>A stop reaches the cases it was asked about and no others.</b> Each case
 * runs under its own configuration, named for the case, so the process behind
 * one is findable by name. Stopping one case while eleven others run leaves the
 * eleven running.
 * <p>
 * Two things have to stop, and killing one of them is not a stop. The
 * <b>process</b> is killed rather than asked to end - see {@link #kill}. The
 * <b>launch that has not happened yet</b> is dropped: running a page of twelve
 * schedules twelve launches, each hopping to a pooled thread and back to the
 * EDT, so most are still queued when the tester presses Stop.
 */
@Service(Service.Level.PROJECT)
public final class TestNGExecution {

    private final @NotNull Project p;

    /**
     * The configurations this plugin launched, by name, so a stop kills what the
     * tester started here and not a build they left going in another tab.
     */
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    /**
     * Cases asked for whose launch has not reached the platform yet. A stop takes
     * a case out, and the launch finds it gone when its turn comes and does not
     * start. An entry lives for the two thread hops between the click and the
     * process, and is taken out by whichever of the two arrives first.
     */
    private final @NotNull Set<TestCaseDto> pending = ConcurrentHashMap.newKeySet();

    /**
     * Cases the tester stopped, so a report arriving afterward is not read as a
     * failure. Cleared by the next run of that case.
     */
    private final @NotNull Set<UUID> stopped = ConcurrentHashMap.newKeySet();

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
     * The run configuration a case runs under: {@code SimpleClass.method}.
     * <p>
     * One owner, because two things have to agree on it - the runner naming the
     * configuration it creates, and a stop looking for the process that
     * configuration started. Worked out from the case alone, so neither has to
     * remember what the other did.
     */
    public static @NotNull String configName(final @NotNull TestCaseDto tc) {
        final List<String> fqcn = Fqcn.ofMethod(tc);
        final String method = fqcn.getLast();
        final String classFqcn = String.join(".", fqcn.subList(0, fqcn.size() - 1));
        final int lastDot = classFqcn.lastIndexOf('.');

        return (lastDot >= 0 ? classFqcn.substring(lastDot + 1) : classFqcn) + "." + method;
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
        pending.add(tc);
        stopped.remove(tc.getId());

        TestCaseExecutionListener.broadcast(p, key(tc), RunStatus.RUNNING, "");
    }

    /**
     * Starts the configuration for a case, unless the tester stopped that case
     * while this launch was still on its way here.
     */
    public void launch(final @NotNull TestCaseDto tc, final @NotNull RunnerAndConfigurationSettings settings) {
        if (!pending.remove(tc)) {
            Logger.info("Not starting " + settings.getName() + ": it was stopped before it began");
            return;
        }

        launch(settings);
    }

    /**
     * Starts a configuration that belongs to no one case - a whole test set run
     * as a class.
     */
    public void launch(final @NotNull RunnerAndConfigurationSettings settings) {
        launchedNames.add(settings.getName());

        Logger.info("Starting " + settings.getName());
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    /**
     * Whether a report arriving for this case belongs to a run the tester
     * stopped.
     * <p>
     * A killed process reports its test as finished without having passed, which
     * reads exactly like a failure. It is not one: nobody found a defect, the
     * case simply did not finish.
     */
    public boolean isStopped(final @NotNull TestCaseDto tc) {
        return stopped.contains(tc.getId());
    }

    /**
     * Kills the processes running these cases and drops their launches that have
     * not started yet.
     * <p>
     * Only the cases that are running, and only those: a selection can hold cases
     * that passed an hour ago, and a stop is no reason to forget their verdicts.
     *
     * @return how many were stopped, for the one notification the gesture makes
     */
    public int stop(final @NotNull List<TestCaseDto> cases) {
        final List<TestCaseDto> stopping = cases.stream()
                .filter(tc -> tc.getTempStatus() == RunStatus.RUNNING)
                .toList();

        stopping.forEach(tc -> {
            pending.remove(tc);
            stopped.add(tc.getId());
        });

        final Set<String> names = stopping.stream()
                .map(TestNGExecution::configName)
                .collect(Collectors.toSet());

        final List<RunContentDescriptor> theirs = running(names);
        Logger.info("Stopping " + stopping.size() + " test case(s): " + theirs.size() + " had reached a process");

        theirs.forEach(this::kill);
        stopping.forEach(tc -> TestCaseExecutionListener.broadcast(p, key(tc), RunStatus.IDLE, ""));

        return stopping.size();
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

    /**
     * The running processes started by these configurations, and only ones this
     * plugin launched - a tester's own configuration of the same name is theirs
     * to stop.
     */
    private @NotNull List<RunContentDescriptor> running(final @NotNull Set<String> names) {
        return ExecutionManager.getInstance(p).getRunningDescriptors(
                settings -> names.contains(settings.getName()) && launchedNames.contains(settings.getName()));
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
