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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Await;
import org.testin.Gestures;
import org.testin.editor.card.CardHoverAction;
import org.testin.editor.EditorFixtures;
import org.testin.editor.toolbar.RefreshBtn;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.Config;
import org.testin.model.status.ExecutionStatus;
import org.testin.model.FileKind;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestCaseDto;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.runner.CapturedRunner;
import org.testin.runner.TestNGExecution;
import org.testin.services.Services;
import org.testin.testrun.TestRunFixture;
import org.testin.util.Mapper;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutomationFromTheTestRunIdeTest extends AbstractCodegenIdeTest {

    private @NotNull CapturedRunner runner = new CapturedRunner();

    @Override
    protected void setUp() {
        super.setUp();
        runner = CapturedRunner.installed(getTestRootDisposable());
    }

    private @NotNull List<TestCaseDto> automatedTestCases(final int count) {
        final @NotNull TestSetDirectoryDto ts = createdTestSet("Checkout");
        final @NotNull List<TestCaseDto> made = new ArrayList<>();
        for (int i = 0; i < count; i++) made.add(createdTestCase(ts, "Test case number " + (i + 1), String.format("m%04d", i)));
        settled();
        return made;
    }

    private @NotNull TestRunDirectoryDto aTestRun(final @NotNull String name, final @NotNull List<TestRunItems> results) {
        final @NotNull TestProjectDirectoryDto tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestRunDirectoryDto tr = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunDirectoryDto made = Services.getInstance(getProject(), DirectoryMapper.class).setTestRunNode(tp.getTestRunsDirectory().getPath().resolve(name), tp.getTestRunsDirectory());
            Services.getInstance(getProject(), Nodes.class).addTestRunDir(made);
            return made;
        });
        Services.getInstance(getProject(), TestRuns.class).putTestRun(tr.getPath(), new TestRunDto().setResults(new ArrayList<>(results)));
        return tr;
    }

    private @NotNull TestRunEditor opened(final @NotNull TestRunDirectoryDto tr) {
        return EditorFixtures.openTestRunEditor(getProject(), tr, getTestRootDisposable());
    }

    private @NotNull TestRunItems resultOf(final @NotNull TestRunDirectoryDto tr, final @NotNull TestCaseDto tc) {
        return Services.getInstance(getProject(), TestRuns.class).getTestRunByPath(tr.getPath()).resultOf(tc.getId()).orElseThrow();
    }

    private void reported(final @NotNull TestCaseDto tc, final @NotNull ExecutionStatus status, final @NotNull Duration duration) {
        CapturedRunner.reports(getProject(), tc, status, duration);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
    }

    private @NotNull Map<?, ?> writtenRunItem(final @NotNull TestRunDirectoryDto tr, final @NotNull TestCaseDto tc) {
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();
        try {
            return Services.getInstance(getProject(), Mapper.class).readValue(Files.readString(tr.getPath().resolve(FileKind.RUN_ITEM.fileName(tc.getId()))), Map.class);
        } catch (final IOException ex) {
            throw new AssertionError("the run item of '" + tc.getDescription() + "' was never written", ex);
        }
    }

    // Rule-EDITOR-PANEL-180
    public void testTheEditorClaimsTheTestCaseSoTheStatusComesBackToItsOwnTestRun() {
        final @NotNull TestCaseDto tc = automatedTestCases(1).getFirst();
        final @NotNull TestRunDirectoryDto running = aTestRun("Cycle-1", List.of(EditorFixtures.pending(tc)));
        final @NotNull TestRunDirectoryDto alsoShowing = aTestRun("Cycle-2", List.of(EditorFixtures.pending(tc)));
        final @NotNull TestRunEditor claiming = opened(running);
        final @NotNull TestRunEditor other = opened(alsoShowing);
        try {
            CardHoverAction.RUN_TEST_METHOD.executeFor(claiming, tc);
            assertEquals("the test case was not handed to TestNG", List.of(List.of(tc)), runner.runs());

            reported(tc, ExecutionStatus.PASSED, Duration.ofMillis(84));

            Await.until("the run item status did not come back to the test run that ran it", () -> resultOf(running, tc).getStatus() == RunItemStatus.PASSED);
            assertEquals("the run item status went to another editor showing the same test case", RunItemStatus.PENDING, resultOf(alsoShowing, tc).getStatus());
        } finally {
            Disposer.dispose(claiming);
            Disposer.dispose(other);
        }
    }

    // Rule-EDITOR-PANEL-182
    public void testARunItemStatusFromTheAutomationIsWrittenAsTheKeyboardsIs() {
        final @NotNull List<TestCaseDto> testCases = automatedTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.getList().setSelectedIndex(0);
            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.PASSED), editor.getList());

            CardHoverAction.RUN_TEST_METHOD.executeFor(editor, testCases.get(1));
            reported(testCases.get(1), ExecutionStatus.PASSED, Duration.ofMillis(84));
            Await.until("the automation's run item status was not recorded", () -> resultOf(tr, testCases.get(1)).getStatus() == RunItemStatus.PASSED);

            final @NotNull TestRunItems byKeyboard = resultOf(tr, testCases.getFirst());
            final @NotNull TestRunItems byAutomation = resultOf(tr, testCases.get(1));
            assertEquals("the automation's run item status was recorded under another tester", byKeyboard.getExecutedBy(), byAutomation.getExecutedBy());
            assertFalse("the automation's run item status says no time it was recorded", Config.isNotExecuted(byAutomation.getExecutedAt()));

            final @NotNull Map<?, ?> keyboardFile = writtenRunItem(tr, testCases.getFirst());
            final @NotNull Map<?, ?> automationFile = writtenRunItem(tr, testCases.get(1));
            assertEquals("the automation's run item status is written in another shape", keyboardFile.keySet(), automationFile.keySet());
            assertEquals(keyboardFile.get("status"), automationFile.get("status"));
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-183
    public void testTheClockThatWasCountingKeepsItsTimeWhenTheAutomationSetsTheRunItemStatus() {
        final @NotNull List<TestCaseDto> testCases = automatedTestCases(2);
        final @NotNull TestRunDirectoryDto tr = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.onStartExecutionClicked();
            Await.until("the walk never started", () -> editor.getWalk().getCurrentlyExecutingIndex() == 0);
            CardHoverAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            final long until = System.currentTimeMillis() + 1300;
            while (System.currentTimeMillis() < until) {
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
                TimeoutUtil.sleep(20);
            }

            reported(testCases.getFirst(), ExecutionStatus.PASSED, Duration.ofMillis(84));

            Await.until("the automation's run item status was not recorded", () -> resultOf(tr, testCases.getFirst()).getStatus() == RunItemStatus.PASSED);
            final @NotNull Duration kept = resultOf(tr, testCases.getFirst()).getDuration();
            assertTrue("the framework's 84 ms replaced the time the clock counted: " + kept, kept.compareTo(Duration.ofMillis(1200)) >= 0);
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-152
    public void testOnlyTheTestersStopOrClosingTheTabEndsTheAutomationThisEditorStarted() {
        final @NotNull List<TestCaseDto> testCases = automatedTestCases(3);
        final @NotNull TestRunDirectoryDto tr = aTestRun("Cycle-1", testCases.stream().map(EditorFixtures::pending).toList());
        final @NotNull TestRunEditor editor = opened(tr);
        final @NotNull TestNGExecution execution = Services.getInstance(getProject(), TestNGExecution.class);
        try {
            CardHoverAction.RUN_TEST_METHOD.executeFor(editor, testCases.getFirst());
            assertTrue(execution.isRunning(testCases.getFirst().getId()));

            editor.getToolBar().getToolbarItem(RefreshBtn.class).doClick();
            Await.until("refresh never finished", () -> editor.run().isPresent());
            editor.getList().setSelectedIndex(1);
            Gestures.press(getProject(), TestRunFixture.keyFor(editor, RunItemStatus.BLOCKED), editor.getList());
            assertTrue("something other than the tester's stop ended the automation", execution.isRunning(testCases.getFirst().getId()));

            editor.onStopExecutionClicked();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            assertFalse("the tester's stop did not end the automation this editor started", execution.isRunning(testCases.getFirst().getId()));

            CardHoverAction.RUN_TEST_METHOD.executeFor(editor, testCases.get(2));
            assertTrue(execution.isRunning(testCases.get(2).getId()));
        } finally {
            Disposer.dispose(editor);
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertFalse("closing the tab did not end the automation this editor started", execution.isRunning(testCases.get(2).getId()));
    }

    // Rule-EDITOR-PANEL-184, Rule-EDITOR-PANEL-185
    public void testRunningTheWholeTestRunHandsOnlyThePendingOnesOverAsOneExecution() {
        final @NotNull List<TestCaseDto> testCases = automatedTestCases(4);
        final @NotNull TestRunDirectoryDto tr = aTestRun("Cycle-1", List.of(
                EditorFixtures.pending(testCases.getFirst()).setStatus(RunItemStatus.PASSED),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.get(2)).setStatus(RunItemStatus.FAILED),
                EditorFixtures.pending(testCases.get(3))));
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("the whole test run was not one execution of the pending test cases only", List.of(List.of(testCases.get(1), testCases.get(3))), runner.runs());

            reported(testCases.getFirst(), ExecutionStatus.FAILED, Duration.ofMillis(5));
            assertEquals("a test case already judged was run again", RunItemStatus.PASSED, resultOf(tr, testCases.getFirst()).getStatus());
        } finally {
            Disposer.dispose(editor);
        }
    }

    // Rule-EDITOR-PANEL-187
    public void testTheMethodsRunInTheOrderOfTheTestSet() {
        final @NotNull List<TestCaseDto> testCases = automatedTestCases(4);
        final @NotNull TestRunDirectoryDto tr = aTestRun("Cycle-1", List.of(
                EditorFixtures.pending(testCases.get(3)),
                EditorFixtures.pending(testCases.get(1)),
                EditorFixtures.pending(testCases.getFirst()),
                EditorFixtures.pending(testCases.get(2))));
        final @NotNull TestRunEditor editor = opened(tr);
        try {
            editor.runWhenLoaded();
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

            assertEquals("the methods were not handed over in the order of the test set", List.of(testCases), runner.runs());
        } finally {
            Disposer.dispose(editor);
        }
    }
}
