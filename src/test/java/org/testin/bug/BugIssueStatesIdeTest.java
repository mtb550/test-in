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

import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.Said;
import org.testin.git.history.BugCard;
import org.testin.git.history.BugHistory;
import org.testin.indexer.TestCaseFile;
import org.testin.indexer.TestRuns;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestRunDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;
import org.testin.view.details.BugIssueLink;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BugIssueStatesIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull UUID TEST_CASE_ID = UUID.fromString("66666666-6666-4666-8666-666666666693");
    private static final @NotNull String FIXED = "https://github.com/mtb550/test-03/issues/9";
    private static final @NotNull String OPEN = "https://github.com/mtb550/test-03/issues/14";
    private static final @NotNull String ANSWER = """
            {"data":{"repository":{"i9":{"state":"CLOSED","stateReason":"COMPLETED"},"i14":{"state":"OPEN","stateReason":null}}}}""";
    private static final @NotNull String NO_PROJECT_SCOPE = """
            {"errors":[{"type":"INSUFFICIENT_SCOPES","message":"The 'title' field requires one of the following scopes: ['read:project']"}]}""";

    private final @NotNull List<List<String>> asked = new ArrayList<>();

    private @NotNull BugIssueStates states() {
        return Services.getInstance(getProject(), BugIssueStates.class);
    }

    private void twoBugsFiled() {
        filed("Cycle 3", FIXED);
        filed("Cycle 4", OPEN);
    }

    private void filed(final @NotNull String testRun, final @NotNull String bugIssueUrl) {
        final @NotNull TestRunItems failed = TestRunItems.builder().id(TEST_CASE_ID).status(RunItemStatus.FAILED).bugIssueUrl(bugIssueUrl).build();
        Services.getInstance(getProject(), TestRuns.class).putTestRun(root.resolve("NAFATH").resolve("Test Runs").resolve(testRun), TestRunDto.builder().results(new ArrayList<>(List.of(failed))).build());
    }

    private void ghAnswers(final @NotNull String withBoards) {
        states().answerWith(new GitHubCli((arguments, _) -> {
            asked.add(arguments);
            return Optional.of(new ProcessOutput(arguments.contains("field=Status") ? withBoards : ANSWER, "", 0, false, false));
        }), getTestRootDisposable());
    }

    private @NotNull List<List<String>> askedAbout(final @NotNull String repository) {
        return asked.stream().filter(arguments -> arguments.contains("name=" + repository)).toList();
    }

    private void readAndWait() {
        final @NotNull AtomicInteger redrawn = new AtomicInteger();
        states().readAll(redrawn::incrementAndGet);
        Await.until("the bug states were never read", () -> redrawn.get() == 1);
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-091, Rule-VIEW-PANEL-092, Rule-VIEW-PANEL-095
    public void testEveryFiledBugGetsItsStateFromOneRequestAndAFixedOneStaysListed() {
        twoBugsFiled();
        ghAnswers(ANSWER);

        readAndWait();

        assertEquals("one repository is one request", 1, askedAbout("test-03").size());
        assertEquals(Bundle.message("bug.state.fixed"), states().of(FIXED).label());
        assertEquals(Bundle.message("bug.state.open"), states().of(OPEN).label());

        final @NotNull List<BugCard> bugs = BugHistory.read(getProject(), new TestCaseFile(root.resolve("NAFATH"), Path.of("Test Cases", "Login", TEST_CASE_ID + ".tc")), TEST_CASE_ID);
        assertEquals("the fixed bug left the history", 2, bugs.size());

        final @NotNull List<String> fixed = Drawn.words(BugIssueLink.of(getProject(), FIXED));
        assertTrue("the fixed bug's pill is missing: " + fixed, Drawn.holds(fixed, "#9") && Drawn.holds(fixed, Bundle.message("bug.state.fixed")));
        final @NotNull List<String> open = Drawn.words(BugIssueLink.of(getProject(), OPEN));
        assertTrue("the open bug shows no pill: " + open, Drawn.holds(open, "#14") && Drawn.holds(open, Bundle.message("bug.state.open")));
    }

    // Rule-VIEW-PANEL-094
    public void testABoardTheTokenCannotReadFallsBackToTheIssueStateAndSaysSoOnce() {
        twoBugsFiled();
        ghAnswers(NO_PROJECT_SCOPE);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        readAndWait();
        readAndWait();

        assertEquals(Bundle.message("bug.state.fixed"), states().of(FIXED).label());
        assertEquals("the permission was asked about more than once", 1, said.stream().filter(shown -> shown.getTitle().equals(Bundle.message("bug.states.board.title"))).count());
        assertEquals("a refused board was asked for again", 2, askedAbout("test-03").stream().filter(arguments -> !arguments.contains("field=Status")).count());
    }

    // Rule-VIEW-PANEL-093
    public void testAGhThatCannotBeStartedLeavesTheLinksAloneAndSaysWhyOnce() {
        twoBugsFiled();
        states().answerWith(new GitHubCli((_, _) -> Optional.empty()), getTestRootDisposable());
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        readAndWait();

        assertEquals(BugIssueState.NOT_READ, states().of(OPEN));
        final int repositories = BugIssueStates.filedIn(Services.getInstance(getProject(), TestRuns.class).getAllTestRuns().values()).size();
        final @NotNull List<Notification> notRead = said.stream().filter(shown -> shown.getTitle().equals(Bundle.message("bug.states.not.read.title"))).toList();
        assertEquals("one message per repository, never one per link", repositories, notRead.size());
        assertTrue(notRead.stream().allMatch(shown -> shown.getContent().equals(Bundle.message("bug.reason.no.gh"))));
    }
}
