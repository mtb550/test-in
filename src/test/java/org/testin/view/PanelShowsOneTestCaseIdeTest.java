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
package org.testin.view;

import org.jetbrains.annotations.NotNull;
import org.testin.Await;
import org.testin.Said;
import org.testin.editor.EditorFixtures;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Locale;

import static org.testin.view.Drawn.holds;

public class PanelShowsOneTestCaseIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String RUN_BAND = Bundle.message("details.band.run").toUpperCase(Locale.ROOT);

    private static @NotNull TestCaseDto handedCopy(final @NotNull TestCaseDto tc, final @NotNull String description) {
        final @NotNull TestCaseDto copy = TestCaseDto.builder().id(tc.getId()).description(description).build();
        copy.setParent(tc.getParent());
        return copy;
    }

    private @NotNull List<TestCaseDto> threeTestCases() {
        final @NotNull TestSetDirectoryDto ts = aTestSet("Login");
        return List.of(aTestCase(ts, "Log in with a valid user", "a"), aTestCase(ts, "Log in with a locked user", "b"), aTestCase(ts, "Log in with no password", "c"));
    }

    private @NotNull List<String> pathOf(final @NotNull TestRunDirectoryDto tr) {
        return tr.getPath2();
    }

    // Rule-VIEW-PANEL-002
    public void testThePanelDrawsTheFirstOfTheTestCasesItWasHandedAndNoOther() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();

        view.getPanel().show(handed, handed.getFirst().getParent().getPath2());

        final @NotNull List<String> words = details();
        assertTrue("the first test case was not drawn: " + words, holds(words, "Log in with a valid user"));
        assertFalse("a second test case was drawn beside the first: " + words, holds(words, "Log in with a locked user"));
        assertFalse("a third test case was drawn beside the first: " + words, holds(words, "Log in with no password"));
    }

    // Rule-VIEW-PANEL-003
    public void testNothingButAskingOpensThePanel() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        view.getPanel().showIfOpen(handed, path);
        ViewToolWindowFactory.refreshIfShowing(getProject(), handed);
        view.getPanel().refreshCurrentView();
        settled();
        assertFalse("the panel opened without the tester asking for a test case's details", view.isOpen());

        view.getPanel().show(handed, path);
        assertTrue("asking for a test case's details did not open the panel", view.isOpen());
    }

    // Rule-VIEW-PANEL-004
    public void testAnOpenPanelFollowsAndAClosedOneStaysClosedUntilAskedAgain() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        view.getPanel().show(List.of(handed.getFirst()), path);
        view.getPanel().showIfOpen(List.of(handed.get(1)), path);
        assertTrue("the open panel did not follow the tester to the next test case: " + details(), holds(details(), "Log in with a locked user"));

        view.closedByTheTester();
        view.getPanel().showIfOpen(List.of(handed.get(2)), path);
        assertFalse("the closed panel came back when the tester moved on", view.isOpen());

        view.getPanel().show(List.of(handed.get(2)), path);
        assertTrue("the tester asked again and the panel stayed closed", view.isOpen());
    }

    // Rule-VIEW-PANEL-005
    public void testThePanelDrawsWhatTestinHoldsNotTheCopyItWasHanded() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();

        view.getPanel().show(List.of(handedCopy(tc, "The copy the editor handed over")), tc.getParent().getPath2());
        assertTrue("the panel drew the copy it was handed rather than what Testin holds: " + details(), holds(details(), "Log in with a valid user"));

        Services.getInstance(getProject(), TestCases.class).putTestCase(tc.getParent().getPath(), handedCopy(tc, "Sign in with a valid user"));
        ViewToolWindowFactory.refreshIfShowing(getProject(), List.of(tc));

        assertTrue("a value changed somewhere else did not reach the panel: " + details(), holds(details(), "Sign in with a valid user"));
    }

    // Rule-VIEW-PANEL-007
    public void testOpeningPagingAndClosingSayNothing() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull List<String> path = handed.getFirst().getParent().getPath2();

        final @NotNull List<String> said = Said.during(getProject(), () -> {
            view.getPanel().show(handed, path);
            view.getPanel().getPage().goNext();
            settled();
            view.getPanel().getPage().goPrevious();
            settled();
            view.getPanel().hide();
            view.getPanel().show(handed, path);
            view.getPanel().hide(path);
        });

        assertEquals("opening, paging or closing the panel said something", List.of(), said);
    }

    // Rule-VIEW-PANEL-008
    public void testEveryRefreshDrawsAllThreeTabs() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());
        assertTrue("the Details tab was not drawn: " + details(), holds(details(), "Log in with a valid user"));
        Await.until("the History tab was not drawn: " + view.words(ViewTab.HISTORY), () -> view.words(ViewTab.HISTORY).equals(List.of(Bundle.message("view.history.not.under.git"))));
        assertEquals("the Open Bugs tab was not drawn for the test case", List.of(Bundle.message("view.bugs.none")), view.words(ViewTab.OPEN_BUGS));

        view.getPanel().reset();
        assertEquals("the Details tab was not drawn again", List.of(Bundle.message("details.placeholder")), details());
        assertEquals("the Open Bugs tab still describes a test case the panel no longer shows", List.of(Bundle.message("view.bugs.no.selection")), view.words(ViewTab.OPEN_BUGS));
        assertEquals("the History tab still shows a test case the panel no longer shows", List.of(), view.words(ViewTab.HISTORY));
    }

    // Rule-VIEW-PANEL-012
    public void testThePanelOpensOnDetailsWhicheverTabWasInFront() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        view.bringToFront(ViewTab.OPEN_BUGS);
        assertEquals(ViewTab.OPEN_BUGS.getDisplayName(), view.tabInFront());

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());

        assertEquals("the panel opened on the tab that was in front last time", ViewTab.DETAILS.getDisplayName(), view.tabInFront());
    }

    // Rule-VIEW-PANEL-014
    public void testTheFolderThePanelWasOpenedFromDecidesWhetherTheTestRunIsDrawn() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(TestRunItems.builder().id(tc.getId()).status(RunItemStatus.FAILED).build()));

        view.getPanel().show(List.of(tc), pathOf(tr));
        assertTrue("opened from the test run, the panel drew no test run band: " + details(), details().contains(RUN_BAND));

        view.getPanel().show(List.of(tc), tc.getParent().getPath2());
        assertFalse("opened from the test set, the panel drew the test run band: " + details(), details().contains(RUN_BAND));
    }

    // Rule-VIEW-PANEL-018
    public void testEveryFillStartsAgainAtTheFirstTestCase() {
        final @NotNull List<TestCaseDto> first = threeTestCases();
        final @NotNull TestSetDirectoryDto other = aTestSet("Logout");
        final @NotNull List<TestCaseDto> second = List.of(aTestCase(other, "Log out from the menu", "a"), aTestCase(other, "Log out by closing the tab", "b"), aTestCase(other, "Log out after a timeout", "c"));

        view.getPanel().show(first, first.getFirst().getParent().getPath2());
        view.getPanel().getPage().goNext();
        view.getPanel().getPage().goNext();
        settled();
        assertTrue("paging did not reach the third test case: " + details(), holds(details(), "Log in with no password"));

        view.getPanel().showIfOpen(second, other.getPath2());

        assertTrue("the new fill did not start at its first test case: " + details(), holds(details(), "Log out from the menu"));
        assertFalse("the paging position was carried over into the new fill", view.getPanel().getPage().hasPrevious());
    }

    // Rule-VIEW-PANEL-029
    public void testTheTestRunBandIsDrawnOnlyWhenTheTestRunHoldsTheTestCase() {
        final @NotNull List<TestCaseDto> handed = threeTestCases();
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(TestRunItems.builder().id(handed.getFirst().getId()).status(RunItemStatus.FAILED).build()));

        view.getPanel().show(List.of(handed.get(1)), pathOf(tr));
        assertFalse("the test run band was drawn for a test case the test run does not hold: " + details(), details().contains(RUN_BAND));
        assertTrue("the test case the test run does not hold was not drawn on its own: " + details(), holds(details(), "Log in with a locked user"));

        view.getPanel().show(List.of(handed.getFirst()), pathOf(tr));
        assertTrue("the test run band was not drawn for a test case the test run holds: " + details(), details().contains(RUN_BAND));
    }

    // Rule-VIEW-PANEL-030
    public void testTheTestRunBandIsReadFromTheTestRunItself() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(EditorFixtures.pending(tc)));
        view.getPanel().show(List.of(tc), pathOf(tr));
        assertTrue(details().contains(RunItemStatus.PENDING.getLabel()));

        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.changeResult(tr.getPath(), tc.getId(), result -> {
            result.setStatus(RunItemStatus.FAILED);
            result.setActualResult("The session was dropped");
        });
        view.getPanel().refreshIfShowing(List.of(tc));

        final @NotNull TestRunItems recorded = testRuns.findTestRun(tr.getPath()).flatMap(run -> run.resultOf(tc.getId())).orElseThrow();
        assertTrue("the band does not show the run item status the test run holds: " + details(), details().contains(recorded.shownStatus().getLabel()));
        assertTrue("the band does not show the actual result the test run holds: " + details(), holds(details(), recorded.getActualResult()));
        assertFalse("the band still shows what it drew before the test run changed: " + details(), details().contains(RunItemStatus.PENDING.getLabel()));
    }

    // Rule-VIEW-PANEL-083
    public void testOpenedFromATestRunThePanelShowsTheTestCaseAsTheRowShowsIt() {
        final @NotNull TestCaseDto tc = threeTestCases().getFirst();
        final @NotNull TestRunItems row = EditorFixtures.pending(tc);
        row.recordRunItemStatus(RunItemStatus.FAILED, "muteb", handedCopy(tc, "Log in as the row recorded it"));
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(row));

        view.getPanel().show(List.of(handedCopy(tc, "The copy the editor handed over")), pathOf(tr));
        assertTrue("the panel did not show the test case as the test run's row shows it: " + details(), holds(details(), "Log in as the row recorded it"));
        assertFalse("the panel showed the copy it was handed: " + details(), holds(details(), "The copy the editor handed over"));

        Services.getInstance(getProject(), TestRuns.class).changeResult(tr.getPath(), tc.getId(), result -> result.recordRunItemStatus(RunItemStatus.PASSED, "muteb", handedCopy(tc, "Log in as the row recorded it again")));
        view.getPanel().refreshCurrentView();

        assertTrue("the panel did not read the row again when it refreshed: " + details(), holds(details(), "Log in as the row recorded it again"));
    }
}
