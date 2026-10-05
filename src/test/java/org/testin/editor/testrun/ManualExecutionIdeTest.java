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

package org.testin.editor.testrun;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.toolbar.StartExecutionBtn;
import org.testin.editor.toolbar.StopExecutionBtn;
import org.testin.indexer.TestRuns;
import org.testin.model.Config;
import org.testin.model.result.Failure;
import org.testin.model.FileKind;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.TestRunStatus;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.testrun.RunItemStatusService;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ManualExecutionIdeTest extends AbstractTempRootIdeTest {

    private @NotNull TestRuns theTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull RunItemStatusService runItemStatuses() {
        return Services.getInstance(getProject(), RunItemStatusService.class);
    }

    private @NotNull List<TestCaseDto> createdTestCases(final int count) {
        final @NotNull TestProjectDirectoryDto tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetDirectoryDto ts = EditorFixtures.testSet(getProject(), tp, "Checkout");
        return EditorFixtures.testCases(getProject(), ts, count);
    }

    private @NotNull TestRunDirectoryDto aTestRunOver(final @NotNull List<TestRunItems> results) {
        final @NotNull TestProjectDirectoryDto tp = EditorFixtures.testProject(getProject(), root);
        return EditorFixtures.testRun(getProject(), tp, results);
    }

    private @NotNull TestRunDirectoryDto aPendingTestRunOver(final @NotNull List<TestCaseDto> testCases) {
        return aTestRunOver(testCases.stream().map(EditorFixtures::pending).toList());
    }

    private @NotNull TestRunEditor opened(final @NotNull TestRunDirectoryDto tr) {
        return EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
    }

    private @NotNull RunItemStatus statusOf(final @NotNull TestRunDirectoryDto tr, final @NotNull TestCaseDto tc) {
        return theTestRuns().getTestRunByPath(tr.getPath()).resultOf(tc.getId()).map(TestRunItems::getStatus).orElseThrow();
    }

    private @NotNull TestRunItems resultOf(final @NotNull TestRunDirectoryDto tr, final @NotNull TestCaseDto tc) {
        return theTestRuns().getTestRunByPath(tr.getPath()).resultOf(tc.getId()).orElseThrow();
    }

    private static @NotNull UUID walkingOn(final @NotNull TestRunEditor editor) {
        final int index = editor.getWalk().getCurrentlyExecutingIndex();
        return index < 0 ? new UUID(0L, 0L) : editor.getCurrentTestCases().get(index).getId();
    }

    private static void awaitWalkOn(final @NotNull TestRunEditor editor, final @NotNull TestCaseDto tc) {
        Await.until("the walk never reached '" + tc.getDescription() + "'", () -> walkingOn(editor).equals(tc.getId()));
    }

    private void record(final @NotNull TestRunEditor editor, final @NotNull RunItemStatus status) {
        runItemStatuses().executeNext(editor, status);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull String fileOf(final @NotNull TestRunDirectoryDto tr, final @NotNull TestCaseDto tc) {
        theTestRuns().awaitWrites();
        try {
            return Files.readString(tr.getPath().resolve(FileKind.RUN_ITEM.fileName(tc.getId())));
        } catch (final IOException ex) {
            throw new AssertionError("the run item of '" + tc.getDescription() + "' was never written", ex);
        }
    }

    private @NotNull List<String> everyFileOf(final @NotNull TestRunDirectoryDto tr) {
        theTestRuns().awaitWrites();
        final @NotNull List<String> read = new ArrayList<>();
        try (final Stream<Path> files = Files.list(tr.getPath())) {
            for (final Path file : files.sorted().toList()) {
                if (Files.isRegularFile(file)) read.add(file.getFileName() + "=" + Files.readString(file));
            }
        } catch (final IOException ex) {
            throw new AssertionError("could not read the test run's folder", ex);
        }
        return read;
    }

    // Rule-EDITOR-PANEL-129
    public void testStartIsOnTheToolbarWhenATestRunOpens() {
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(createdTestCases(2)));
        try {
            final @NotNull StartExecutionBtn start = editor.getToolBar().getToolbarItem(StartExecutionBtn.class);

            assertTrue("Start Manual Execution is not on the toolbar of a test run just opened", start.isVisible());
            assertTrue("Start Manual Execution is gray on a test run with test cases waiting", start.isEnabled());
            assertFalse("Stop Execution is offered on a test run that is not executing", editor.getToolBar().getToolbarItem(StopExecutionBtn.class).isVisible());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-130, Rule-EDITOR-PANEL-139, Rule-EDITOR-PANEL-153
    public void testTheWalkLandsOnlyOnTestCasesWaitingForARunItemStatus() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(4);
        final @NotNull TestRunItems judgedBySomeoneElse = EditorFixtures.pending(testCases.get(0)).setStatus(RunItemStatus.FAILED).setExecutedBy("Sara");
        final @NotNull TestRunItems judgedInTheFirstSitting = EditorFixtures.pending(testCases.get(2)).setStatus(RunItemStatus.PASSED);
        final @NotNull TestRunEditor editor = opened(aTestRunOver(List.of(judgedBySomeoneElse, EditorFixtures.pending(testCases.get(1)), judgedInTheFirstSitting, EditorFixtures.pending(testCases.get(3)))));
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.get(1));

            record(editor, RunItemStatus.PASSED);

            awaitWalkOn(editor, testCases.get(3));
            assertEquals("the run item status was not recorded on the test case the walk was on", RunItemStatus.PASSED, editor.runItem(testCases.get(1).getId()).orElseThrow().getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-131
    public void testStartingMarksTheTestRunInProgressAndStampsWhenExecutionBegan() {
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(createdTestCases(2));
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            assertTrue("a test run nobody started already says when execution began", Config.isNotExecuted(tr.getMarker().getExecutionStartedAt()));

            editor.onStartExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(TestRunStatus.IN_PROGRESS, editor.getParent().getMarker().getStatus());
            assertFalse("starting did not stamp when execution began", Config.isNotExecuted(editor.getParent().getMarker().getExecutionStartedAt()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-133
    public void testTheButtonBecomesStopWhileTheWalkIsGoing() {
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(createdTestCases(2)));
        try {
            editor.onStartExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertTrue("Stop Execution is not shown while the walk is going", editor.getToolBar().getToolbarItem(StopExecutionBtn.class).isVisible());
            assertFalse("Start is still shown while the walk is going", editor.getToolBar().getToolbarItem(StartExecutionBtn.class).isVisible());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-134
    public void testReachingTheEndOfTheListEndsTheWalkAndNothingMore() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.getToolBar().getSearchTxt().setText(testCases.getFirst().getDescription());
            Await.until("the search never narrowed the list", () -> editor.getCurrentTestCases().size() == 1);

            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.getFirst());
            record(editor, RunItemStatus.PASSED);

            Await.until("the walk did not end at the end of the list", () -> !editor.getWalk().isExecuting());
            assertNotSame("the test run was marked Completed with a test case still waiting", TestRunStatus.COMPLETED, editor.getParent().getMarker().getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-134, Rule-EDITOR-PANEL-186
    public void testTheTestRunIsCompletedOnceEveryTestCaseInItIsJudged() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(testCases));
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.get(0));
            record(editor, RunItemStatus.PASSED);
            awaitWalkOn(editor, testCases.get(1));

            assertEquals("the test run was Completed while a test case still waited", TestRunStatus.IN_PROGRESS, editor.getParent().getMarker().getStatus());

            record(editor, RunItemStatus.BLOCKED);

            Await.until("the test run was not marked Completed when its last test case was judged", () -> editor.getParent().getMarker().getStatus() == TestRunStatus.COMPLETED);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-135
    public void testStartIsGrayWhenEveryTestCaseHasBeenJudged() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunEditor editor = opened(aTestRunOver(List.of(
                EditorFixtures.pending(testCases.get(0)).setStatus(RunItemStatus.PASSED),
                EditorFixtures.pending(testCases.get(1)).setStatus(RunItemStatus.BLOCKED))));
        try {
            assertFalse("Start is offered on a test run with nothing to walk", editor.getToolBar().getToolbarItem(StartExecutionBtn.class).isEnabled());

            editor.onStartExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertFalse("a press that reached Testin anyway started a walk", editor.getWalk().isExecuting());
            assertEquals("a refused start still marked the test run In Progress", TestRunStatus.CREATED, editor.getParent().getMarker().getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-135
    public void testStartIsGrayWhenTheFilterMatchesNothing() {
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(createdTestCases(2)));
        try {
            editor.getToolBar().getSearchTxt().setText("no test case reads this");
            Await.until("the search never emptied the list", () -> editor.getCurrentTestCases().isEmpty());

            assertFalse("Start is offered with a filter that matches nothing", editor.getToolBar().getToolbarItem(StartExecutionBtn.class).isEnabled());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-149, Rule-EDITOR-PANEL-151
    public void testStoppingWritesTheTestRunAsItStandsAndStampsTheEnd() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.getFirst());
            TimeoutUtil.sleep(1100);

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            final @NotNull TestRunItems written = Services.getInstance(getProject(), Mapper.class).readValue(fileOf(tr, testCases.getFirst()), TestRunItems.class);
            assertTrue("the time the clock counted was not written down by the stop: " + written.getDuration(), written.getDuration().compareTo(Duration.ofSeconds(1)) >= 0);
            assertFalse("stopping did not stamp when execution ended", Config.isNotExecuted(editor.getParent().getMarker().getExecutionEndedAt()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-150
    public void testStoppingChangesNoRunItemStatusAndNoStatus() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.get(0));
            record(editor, RunItemStatus.FAILED);
            awaitWalkOn(editor, testCases.get(1));
            final @NotNull ZonedDateTime began = editor.getParent().getMarker().getExecutionStartedAt();

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(RunItemStatus.FAILED, statusOf(tr, testCases.get(0)));
            assertEquals("stopping judged the test case the walk was on", RunItemStatus.PENDING, statusOf(tr, testCases.get(1)));
            assertEquals(RunItemStatus.PENDING, statusOf(tr, testCases.get(2)));
            assertEquals("stopping moved the test run's status", TestRunStatus.IN_PROGRESS, editor.getParent().getMarker().getStatus());
            assertEquals("stopping stamped when execution began", began, editor.getParent().getMarker().getExecutionStartedAt());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-156, Rule-EDITOR-PANEL-157, Rule-EDITOR-PANEL-158
    public void testARunItemStatusRecordedAwayFromTheWalkIsNotTimedAndLeavesTheWalkAlone() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aTestRunOver(List.of(
                EditorFixtures.pending(testCases.get(0)),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.get(2)).setDuration(Duration.ofSeconds(7))));
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.get(0));
            TimeoutUtil.sleep(1100);

            runItemStatuses().applyStatus(editor, List.of(testCases.get(2)), RunItemStatus.BLOCKED);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("a run item status could not be recorded away from the walk", RunItemStatus.BLOCKED, statusOf(tr, testCases.get(2)));
            assertEquals("a run item status recorded away from the walk was timed", Duration.ofSeconds(7), resultOf(tr, testCases.get(2)).getDuration());
            assertTrue("recording away from the walk stopped it", editor.getWalk().isExecuting());
            assertEquals("recording away from the walk moved it", testCases.get(0).getId(), walkingOn(editor));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-156
    public void testARunItemStatusCanBeRecordedWithNoWalkGoing() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            runItemStatuses().applyStatus(editor, List.of(testCases.get(1)), RunItemStatus.FAILED);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(RunItemStatus.FAILED, statusOf(tr, testCases.get(1)));
            assertFalse("recording a run item status started a walk", editor.getWalk().isExecuting());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-163, Rule-EDITOR-PANEL-164
    public void testSeveralAtOnceSkipsTheRemovedOnesAndTimesNone() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull UUID removed = UUID.randomUUID();
        final @NotNull TestRunDirectoryDto tr = aTestRunOver(List.of(
                EditorFixtures.pending(testCases.get(0)),
                EditorFixtures.pending(testCases.get(1)),
                new TestRunItems().setId(removed)));
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            final @NotNull List<TestCaseDto> everyRow = new ArrayList<>(editor.getAllTestCases());
            assertEquals("the removed test case has no row", 3, everyRow.size());

            runItemStatuses().applyStatus(editor, everyRow, RunItemStatus.BLOCKED);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(RunItemStatus.BLOCKED, statusOf(tr, testCases.get(0)));
            assertEquals(RunItemStatus.BLOCKED, statusOf(tr, testCases.get(1)));
            assertEquals("a test case removed from its test set was given the run item status", RunItemStatus.PENDING, theTestRuns().getTestRunByPath(tr.getPath()).resultOf(removed).orElseThrow().getStatus());
            assertEquals("a run item status given to several at once was timed", Duration.ZERO, resultOf(tr, testCases.get(0)).getDuration());
            assertEquals(Duration.ZERO, resultOf(tr, testCases.get(1)).getDuration());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-227
    public void testTheWalkFollowsTheTestCaseBeingExecutedNotItsPlace() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.get(0));

            editor.getToolBar().getSearchTxt().setText(testCases.get(2).getDescription());
            Await.until("the search never narrowed the list", () -> editor.getCurrentTestCases().size() == 1);

            record(editor, RunItemStatus.PASSED);

            assertEquals("a run item status key moved onto the test case left in view", RunItemStatus.PENDING, statusOf(tr, testCases.get(2)));
            assertEquals("the hidden test case being executed was judged", RunItemStatus.PENDING, statusOf(tr, testCases.get(0)));

            editor.getToolBar().getSearchTxt().setText("");
            Await.until("clearing the filter did not bring the test case being executed back", () -> walkingOn(editor).equals(testCases.getFirst().getId()));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-125
    public void testOpeningATestRunNeverRewritesIt() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aTestRunOver(List.of(
                EditorFixtures.pending(testCases.get(0)).setStatus(RunItemStatus.PASSED),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.get(2))));
        final @NotNull List<String> before = everyFileOf(tr);

        final @NotNull TestRunEditor editor = opened(tr);
        try {
            assertEquals("opening the test run rewrote it", before, everyFileOf(tr));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-127
    public void testTheTestCasesAreDrawnInTheOrderOfTheirTestSet() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(List.of(testCases.get(2), testCases.get(0), testCases.get(1))));
        try {
            assertEquals(testCases.stream().map(TestCaseDto::getId).toList(), editor.getAllTestCases().stream().map(TestCaseDto::getId).toList());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-181
    public void testClaimingATestCaseForTheAutomationMarksTheTestRunInProgress() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(2);
        final @NotNull TestRunEditor editor = opened(aPendingTestRunOver(testCases));
        try {
            editor.launching(testCases.getFirst().getId());
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(TestRunStatus.IN_PROGRESS, editor.getParent().getMarker().getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-242
    public void testAStatusFromTheAutomationMovesTheWalkOnAsTheTestersOwnDoes() {
        final @NotNull List<TestCaseDto> testCases = createdTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aPendingTestRunOver(testCases);
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            awaitWalkOn(editor, testCases.getFirst());

            runItemStatuses().recordReported(editor, testCases.getFirst(), RunItemStatus.PASSED, Duration.ofMillis(84), Failure.NONE);
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals(RunItemStatus.PASSED, statusOf(tr, testCases.get(0)));
            awaitWalkOn(editor, testCases.get(1));
            assertTrue("execution did not go on", editor.getWalk().isExecuting());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
