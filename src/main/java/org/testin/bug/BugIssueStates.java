/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.bug;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.testin.help.Hint;
import org.testin.help.Hints;
import org.testin.help.SetupStep;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunDto;
import org.testin.model.bug.BugIssue;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.result.TestRunItems;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Mapper;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class BugIssueStates {
    private final @NotNull Project p;
    private final @NotNull Map<BugIssue, BugIssueState> answers = new ConcurrentHashMap<>();
    private final @NotNull AtomicBoolean reading = new AtomicBoolean();
    private volatile @NotNull Optional<Function<ProgressIndicator, GitHubCli>> gh;

    public BugIssueStates(final @NotNull Project p) {
        this.p = p;
        this.gh = ApplicationManager.getApplication().isUnitTestMode() ? Optional.empty() : Optional.of(GitHubCli::onPath);
    }

    static @NotNull Map<BugRepository, Set<Integer>> filedIn(final @NotNull Collection<TestRunDto> testRuns) {
        return testRuns.stream()
                .flatMap(testRun -> testRun.getResults().stream())
                .map(TestRunItems::bugIssue)
                .flatMap(Optional::stream)
                .map(BugIssueUrl::issue)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(issue -> new BugRepository(issue.host(), issue.owner(), issue.name()),
                        Collectors.mapping(BugIssue::number, Collectors.toCollection(TreeSet::new))));
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-091
    public @NotNull BugIssueState of(final @NotNull String bugIssueUrl) {
        return BugIssueUrl.issue(bugIssueUrl).map(issue -> answers.getOrDefault(issue, BugIssueState.NOT_READ)).orElse(BugIssueState.NOT_READ);
    }

    // UC-VIEW-PANEL-005, UC-VIEW-PANEL-007, Rule-VIEW-PANEL-092
    public void readAll(final @NotNull Runnable redraw) {
        final @NotNull Map<BugRepository, Set<Integer>> filed = filedIn(Services.getInstance(p, TestRuns.class).getAllTestRuns().values());
        if (filed.isEmpty() || gh.isEmpty() || !reading.compareAndSet(false, true)) return;

        final @NotNull Function<ProgressIndicator, GitHubCli> cli = gh.orElseThrow();
        final @NotNull AtomicBoolean changed = new AtomicBoolean();
        BackgroundWork.start(new Task.Backgroundable(p, Bundle.message("bug.states.reading"), true) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                for (final Map.Entry<BugRepository, Set<Integer>> repository : filed.entrySet()) {
                    indicator.checkCanceled();
                    indicator.setText2(repository.getKey().displayName());
                    if (read(cli.apply(indicator), repository.getKey(), repository.getValue())) changed.set(true);
                }
            }

            @Override
            public void onFinished() {
                reading.set(false);
                if (changed.get()) redraw.run();
            }
        });
    }

    // Rule-VIEW-PANEL-093, Rule-VIEW-PANEL-094, Rule-INTERNAL-127
    private boolean read(final @NotNull GitHubCli cli, final @NotNull BugRepository repository, final @NotNull Set<Integer> numbers) {
        final @NotNull Mapper mapper = Services.getInstance(p, Mapper.class);
        final @NotNull Hints hints = Services.getInstance(p, Hints.class);
        final @NotNull IssueStates asked = cli.states(mapper, repository, numbers, true);
        final @NotNull IssueStates said = asked.boardRefused() ? withoutBoards(cli, mapper, repository, numbers) : asked;

        if (!said.problem().isEmpty()) {
            hints.fire(Hint.of(SetupStep.BUG_STATES, said.problem()));
            return false;
        }

        hints.clear(SetupStep.BUG_STATES);
        if (!asked.boardRefused()) hints.clear(SetupStep.BOARD_COLUMNS);
        boolean changed = false;
        for (final int number : numbers) changed |= remember(repository, number, said.stateOf(number));
        return changed;
    }

    // Rule-VIEW-PANEL-092
    private boolean remember(final @NotNull BugRepository repository, final int number, final @NotNull BugIssueState state) {
        return !state.equals(answers.put(new BugIssue(repository.host(), repository.owner(), repository.name(), number), state));
    }

    // Rule-VIEW-PANEL-094, Rule-INTERNAL-127
    private @NotNull IssueStates withoutBoards(final @NotNull GitHubCli cli, final @NotNull Mapper mapper, final @NotNull BugRepository repository, final @NotNull Set<Integer> numbers) {
        Services.getInstance(p, Hints.class).fire(Hint.of(SetupStep.BOARD_COLUMNS, Bundle.message("bug.states.board.message", repository.host())));
        return cli.states(mapper, repository, numbers, false);
    }

    @TestOnly
    boolean isReading() {
        return reading.get();
    }

    @TestOnly
    void answerWith(final @NotNull GitHubCli fake, final @NotNull Disposable until) {
        final @NotNull Optional<Function<ProgressIndicator, GitHubCli>> before = gh;
        gh = Optional.of(_ -> fake);
        answers.clear();
        Disposer.register(until, () -> gh = before);
    }
}
