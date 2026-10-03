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
package org.testin.view.details;

import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.BrowserOpened;
import org.testin.view.Drawn;
import org.testin.view.bugs.OpenBugsTab;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Optional;

public class BugOnTheSummaryLineIdeTest extends AbstractViewPanelIdeTest {

    private static final @NotNull String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private TestCaseDto tc;
    private TestRunDirectoryDto tr;

    @Override
    protected void setUp() {
        super.setUp();
        tc = aTestCase(aTestSet("Login"), "Log in with a valid user", "a");
        tr = aTestRun(List.of(TestRunItems.builder().id(tc.getId()).build()));
    }

    private static @NotNull TestRunItems recorded(final @NotNull TestCaseDto tc, final @NotNull RunItemStatus status, final @NotNull String bugIssueUrl) {
        return TestRunItems.builder().id(tc.getId()).status(status).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl(bugIssueUrl).build();
    }

    private static @NotNull Component rowOf(final @NotNull Component drawn) {
        Component row = drawn;
        while (!(row.getParent().getLayout() instanceof GridBagLayout)) row = row.getParent();
        return row;
    }

    private @NotNull JBPanel<?> drawn(final @NotNull TestRunItems runItem) {
        return Drawn.detailsTab(getProject(), tc, Optional.of(runItem), tr.getPath2());
    }

    // Rule-VIEW-PANEL-066
    public void testAFailedTestCaseCarriesItsBugAndReportBugOnTheSummaryLine() {
        final @NotNull JBPanel<?> tab = drawn(recorded(tc, RunItemStatus.FAILED, ""));

        final @NotNull Component status = Drawn.reading(tab, RunItemStatus.FAILED.getLabel());
        final @NotNull Component report = Drawn.reading(tab, Bundle.message("bug.dialog.title"));
        assertTrue("Report Bug is not a link", report instanceof ActionLink);
        assertSame("Report Bug is not on the summary line", rowOf(status), rowOf(report));
        assertTrue("the bug's chip is not on the summary line", Drawn.words(tab).stream().anyMatch(word -> word.startsWith(BugSeverity.MAJOR.getLabel())));
    }

    // Rule-VIEW-PANEL-066
    public void testTheBugStaysWhileTheTestCaseHasAnIssueWhateverItsRunItemStatus() {
        for (final RunItemStatus status : List.of(RunItemStatus.PASSED, RunItemStatus.BLOCKED, RunItemStatus.FAILED)) {
            final @NotNull JBPanel<?> tab = drawn(recorded(tc, status, ISSUE));
            final @NotNull Component issue = Drawn.reading(tab, "#412");
            assertSame("the issue of a " + status.getLabel() + " test case is not on the summary line", rowOf(Drawn.reading(tab, status.getLabel())), rowOf(issue));
        }
        assertFalse("a passed test case with no issue still carries a bug", Drawn.words(drawn(recorded(tc, RunItemStatus.PASSED, ""))).contains(Bundle.message("bug.dialog.title")));
    }

    // Rule-VIEW-PANEL-075
    public void testAReportedTestCaseShowsItsIssueAsANumberThatOpensIt() {
        final @NotNull BrowserOpened browser = BrowserOpened.recording(getTestRootDisposable());
        final @NotNull JBPanel<?> tab = drawn(recorded(tc, RunItemStatus.FAILED, ISSUE));

        final @NotNull ActionLink issue = (ActionLink) Drawn.reading(tab, "#412");
        assertTrue("hovering the issue does not give its whole address: " + issue.getToolTipText(), String.valueOf(issue.getToolTipText()).contains(ISSUE));
        assertFalse("the repository is repeated on the row: " + Drawn.words(tab), Drawn.holds(Drawn.words(tab), "mtb550/test-in"));
        assertFalse("Report Bug is offered beside the issue it was reported as: " + Drawn.words(tab), Drawn.words(tab).contains(Bundle.message("bug.dialog.title")));

        issue.doClick();
        assertEquals("clicking the issue did not open it in the browser", List.of(ISSUE), browser.addresses());

        Services.getInstance(getProject(), TestRuns.class).changeResult(tr.getPath(), tc.getId(), result -> {
            result.setStatus(RunItemStatus.FAILED);
            result.linkBug(ISSUE);
        });
        final @NotNull JBPanel<?> bugs = new JBPanel<>();
        new OpenBugsTab().load(getProject(), bugs, Optional.of(tc));
        final @NotNull ActionLink listed = (ActionLink) Drawn.reading(bugs, "#412");
        assertTrue("hovering the issue on Open Bugs does not give its whole address: " + listed.getToolTipText(), String.valueOf(listed.getToolTipText()).contains(ISSUE));

        listed.doClick();
        assertEquals("clicking the issue on Open Bugs did not open it in the browser", List.of(ISSUE, ISSUE), browser.addresses());
    }
}
