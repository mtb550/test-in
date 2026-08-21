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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The TestNG runs this plugin started: which cases are running under which
 * configuration, and how to stop the ones the tester asked to stop.
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
     * one out, and the launch finds it gone when its turn comes. An entry lives
     * for the two thread hops between the click and the process.
     */
    private final @NotNull Set<TestCaseDto> pending = ConcurrentHashMap.newKeySet();

    /**
     * Which run each case is running under, by the configuration's name. This is
     * what lets a stop reach one run without touching another, and what tells it
     * which other cases go down with the process it kills.
     */
    private final @NotNull Map<TestCaseDto, String> configOf = new ConcurrentHashMap<>();

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
     * Which of these cases the tester still wants run, taking them out of the
     * queue as it answers.
     * <p>
     * Asked immediately before the configuration is built, so a case stopped
     * while the run was being prepared is left out of it rather than started and
     * then killed.
     */
    public @NotNull List<TestCaseDto> stillWanted(final @NotNull List<TestCaseDto> cases) {
        return cases.stream().filter(pending::remove).toList();
    }

    /**
     * Starts the configuration, and remembers which cases are running under it.
     */
    public void launch(final @NotNull List<TestCaseDto> cases, final @NotNull RunnerAndConfigurationSettings settings) {
        cases.forEach(tc -> configOf.put(tc, settings.getName()));
        launchedNames.add(settings.getName());

        Logger.info("Starting " + settings.getName() + " with " + cases.size() + " test case(s)");
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    /**
     * A case that was asked for is not going to run. Drops it from the queue and
     * puts the card back, saying nothing - for the paths that have already told
     * the tester why in their own words.
     */
    public void notStarting(final @NotNull TestCaseDto tc) {
        pending.remove(tc);
        configOf.remove(tc);

        TestCaseExecutionListener.broadcast(p, key(tc), RunStatus.IDLE, "");
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
     * <p>
     * A killed process reports its test as finished without having passed, which
     * reads exactly like a failure. It is not one: nobody found a defect, the
     * case simply did not finish.
     */
    public boolean isStopped(final @NotNull TestCaseDto tc) {
        return stopped.contains(tc.getId());
    }

    /**
     * Kills the runs these cases belong to and drops their launches that have not
     * started yet.
     * <p>
     * Only the cases that are running: a selection can hold cases that passed an
     * hour ago, and a stop is no reason to forget their verdicts.
     *
     * @return how many cases were stopped, which is more than were asked for when
     *         they share a run - the count the one notification reports
     */
    public int stop(final @NotNull List<TestCaseDto> cases) {
        final @NotNull List<TestCaseDto> asked = cases.stream()
                .filter(tc -> tc.getTempStatus() == RunStatus.RUNNING)
                .toList();

        if (asked.isEmpty()) return 0;

        // One configuration is one process, however many cases it holds, so a
        // case cannot be stopped without stopping the ones beside it. Leaving
        // those showing Running would be the older bug in a smaller place.
        final @NotNull Set<String> runs = asked.stream()
                .map(tc -> configOf.getOrDefault(tc, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<TestCaseDto> stopping = Stream.concat(
                        asked.stream(),
                        configOf.entrySet().stream().filter(e -> runs.contains(e.getValue())).map(Map.Entry::getKey))
                .distinct()
                .filter(tc -> tc.getTempStatus() == RunStatus.RUNNING)
                .toList();

        stopping.forEach(tc -> {
            pending.remove(tc);
            configOf.remove(tc);
            stopped.add(tc.getId());
        });

        final @NotNull List<RunContentDescriptor> theirs = running(runs);
        Logger.info("Stopping " + stopping.size() + " test case(s) in " + runs.size()
                + " run(s): " + theirs.size() + " had reached a process");

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
