package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What this plugin's test runs are doing, by case id: which cases are on their
 * way to a process, which are running under one, which the tester stopped, and
 * what the last report said about each.
 * <p>
 * <b>Everything here is keyed by the case's id, never by the DTO.</b> An indexer
 * rescan replaces every DTO instance, so state kept against an object could no
 * longer be reached the moment the tree reloaded - a stop found nothing to kill
 * and the run went on (#116). The id survives the reload.
 * <p>
 * <b>The one owner of what a surface paints for a case.</b> The card, the
 * details badge, the run/stop slot and the double-run guard all used to work
 * this out from a field on the DTO, and all four went blind together on a
 * rescan. They ask {@link #statusOf} now, which answers from state that outlives
 * the instance.
 * <p>
 * Nothing here touches the platform, which is the point: {@link TestNGExecution}
 * owns the launching, the killing and the reporting, and this owns the answers.
 * A run that cannot be started without an IDE can be reasoned about - and
 * tested - without one.
 */
final class RunRegistry {

    /**
     * The configurations this plugin launched, by name, so a stop kills what the
     * tester started here and not a build they left going in another tab.
     * Emptied by {@link #ended} as each run's process finishes.
     */
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    /**
     * Cases asked for whose launch has not reached the platform yet. A stop takes
     * one out, and the launch finds it gone when its turn comes. An entry lives
     * for the two thread hops between the click and the process.
     */
    private final @NotNull Set<UUID> pending = ConcurrentHashMap.newKeySet();

    /**
     * Which run each case is running under, by the configuration's name. This is
     * what lets a stop reach one run without touching another, and what tells it
     * which other cases go down with the process it kills.
     * <p>
     * An entry here is a case whose result is not in: a report retires its entry
     * the moment it lands, so nothing that reads this can sweep up a verdict
     * already given.
     */
    private final @NotNull Map<UUID, String> configOf = new ConcurrentHashMap<>();

    /**
     * Cases the tester stopped, so a report arriving afterward is not read as a
     * failure. Cleared by the next run of that case.
     * <p>
     * Left to grow for the session on purpose. It is bounded by the number of
     * distinct cases a tester stopped and never ran again, and the alternative -
     * dropping an entry once its report has been seen - would read the second
     * report of a killed run as a real failure, which is the bug this set exists
     * to prevent (#34).
     */
    private final @NotNull Set<UUID> stopped = ConcurrentHashMap.newKeySet();

    /**
     * What the last report said about each case.
     * <p>
     * Here rather than on the DTO because a verdict has to outlive a rescan: a
     * case that passed still shows Passed after the tester edits a file. The
     * field this replaces was dropped on every reload along with the instance
     * that held it, so the green badge vanished at the next keystroke.
     */
    private final @NotNull Map<UUID, RunStatus> verdict = new ConcurrentHashMap<>();

    /**
     * A case is on its way to the runner. Any stop it carried from a previous
     * run is spent, because this is a new one.
     */
    void starting(final @NotNull UUID id) {
        pending.add(id);
        stopped.remove(id);
    }

    /**
     * Whether this case is still wanted, taking it out of the queue as it
     * answers. False for one the tester stopped while the run was being
     * prepared, which is then left out of the configuration rather than started
     * and killed a moment later.
     */
    boolean take(final @NotNull UUID id) {
        return pending.remove(id);
    }

    /**
     * These cases are now running under this configuration.
     */
    void launched(final @NotNull Collection<UUID> ids, final @NotNull String runName) {
        ids.forEach(id -> configOf.put(id, runName));
        launchedNames.add(runName);
    }

    /**
     * Whether this plugin started the run of this name, and it has not ended.
     * A tester's own configuration of the same name is theirs to stop.
     */
    boolean launchedHere(final @NotNull String runName) {
        return launchedNames.contains(runName);
    }

    /**
     * A case that was asked for is not going to run after all.
     */
    void notStarting(final @NotNull UUID id) {
        pending.remove(id);
        configOf.remove(id);
    }

    /**
     * Whether a report arriving for this case belongs to a run the tester
     * stopped.
     * <p>
     * A killed process reports its test as finished without having passed, which
     * reads exactly like a failure. It is not one: nobody found a defect, the
     * case simply did not finish (#34).
     */
    boolean isStopped(final @NotNull UUID id) {
        return stopped.contains(id);
    }

    /**
     * Whether a stop has something to reach for this case: a launch on its way,
     * or a process it is running under.
     */
    boolean isRunning(final @NotNull UUID id) {
        return pending.contains(id) || configOf.containsKey(id);
    }

    /**
     * The status a surface paints for this case: Running while this registry
     * holds it, otherwise whatever the last report said, and
     * {@link RunStatus#IDLE} for a case nobody has run - which draws no badge at
     * all, so absence needs no check at any call site.
     */
    @NotNull RunStatus statusOf(final @NotNull UUID id) {
        return isRunning(id) ? RunStatus.RUNNING : verdict.getOrDefault(id, RunStatus.IDLE);
    }

    /**
     * A report landed with this case's result. The verdict is what every surface
     * paints from now on, and the case stops counting as running - which is what
     * keeps {@link #isRunning} honest, and what keeps a case that finished early
     * out of the blast radius when the run they shared is stopped later.
     * <p>
     * A report of {@link RunStatus#RUNNING} is not a result and is not recorded:
     * the case is already known to be running, by the entry that put it here.
     */
    void reported(final @NotNull UUID id, final @NotNull RunStatus status) {
        if (status == RunStatus.RUNNING) return;

        verdict.put(id, status);
        pending.remove(id);
        configOf.remove(id);
    }

    /**
     * Everything a stop of these cases takes down, and the record that it
     * happened.
     * <p>
     * The casemates come with them: one configuration is one process, so a case
     * in a run of twelve cannot be stopped without stopping the eleven beside
     * it, and leaving those showing Running would be the older bug in a smaller
     * place. They come straight from {@link #configOf}, so a case that already
     * reported is not swept up with them.
     */
    @NotNull Stop stopping(final @NotNull List<UUID> asked) {
        final @NotNull List<UUID> running = asked.stream().filter(this::isRunning).toList();
        if (running.isEmpty()) return Stop.NOTHING;

        final @NotNull Set<String> runs = running.stream()
                .map(id -> configOf.getOrDefault(id, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<UUID> cases = Stream.concat(
                        running.stream(),
                        configOf.entrySet().stream().filter(e -> runs.contains(e.getValue())).map(Map.Entry::getKey))
                .distinct()
                .toList();

        cases.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
            stopped.add(id);
        });

        return new Stop(runs, cases);
    }

    /**
     * A run's process has ended: the cases still recorded under it are put back,
     * and the run stops being one this plugin is holding.
     * <p>
     * Whatever ended it. A build that failed before a single test reported, a
     * crashed JVM, or the IDE's own Stop button in the Run tool window all end a
     * process without a verdict this registry would otherwise hear - and a case
     * left in {@link #configOf} reads as running for the rest of the session,
     * because a verdict and our own stop were the only two things that ever
     * cleared it.
     *
     * @return the cases the run left behind, which is empty for a run that ended
     *         with every result in - and for one this plugin did not start
     */
    @NotNull List<UUID> ended(final @NotNull String runName) {
        if (!launchedNames.remove(runName)) return List.of();

        final @NotNull List<UUID> abandoned = configOf.entrySet().stream()
                .filter(e -> e.getValue().equals(runName))
                .map(Map.Entry::getKey)
                .toList();

        abandoned.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
        });

        return abandoned;
    }

    /**
     * What one stop takes down: the runs whose processes are killed, and every
     * case put back because of it.
     * <p>
     * One value rather than two, because they are one answer - the runs without
     * the cases kills processes and leaves cards showing Running, and the cases
     * without the runs puts the cards back while the tests keep going.
     */
    record Stop(@NotNull Set<String> runs, @NotNull List<UUID> cases) {

        /**
         * The stop of nothing: no case asked for was running. Never kills and
         * never repaints, and here so the empty answer has a value of its own
         * type instead of a check at the caller.
         */
        static final @NotNull Stop NOTHING = new Stop(Set.of(), List.of());
    }
}
