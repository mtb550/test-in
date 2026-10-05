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

package org.testin.view.history;

import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.editor.open.TestinEditors;
import org.testin.editor.testrun.TestRunEditor;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.TestRunDto;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.node.TestSetDirectoryDto;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HistoryTabIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull JBPanel<?> tab = new JBPanel<>(new BorderLayout());

    private @NotNull TestCaseDto aTestCase() {
        return EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Login"), "Log in with a valid user", "a");
    }

    private static @NotNull TestRunItems failed(final @NotNull TestCaseDto tc) {
        return TestRunItems.builder().id(tc.getId()).status(RunItemStatus.FAILED).actualResult("The basket was emptied").bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").build();
    }

    // UC-VIEW-PANEL-007, Rule-VIEW-PANEL-100, Rule-VIEW-PANEL-105, Rule-VIEW-PANEL-109
    public void testABugIsACardOfItsOwnWithItsAttributesAndItsTestRunOpensTheRunItem() {
        final @NotNull TestProjectDirectoryDto tp = EditorFixtures.testProject(getProject(), root);
        final @NotNull TestSetDirectoryDto ts = EditorFixtures.testSet(getProject(), tp, "Login");
        final @NotNull TestCaseDto tc = EditorFixtures.testCase(getProject(), ts, "Log in with a valid user", "a");
        final @NotNull TestRunDirectoryDto tr = EditorFixtures.testRun(getProject(), tp, List.of(failed(tc)));
        Services.getInstance(getProject(), TestRuns.class).putTestRun(tp.getTestRunsDirectory().getPath().resolve("Cycle-0"), TestRunDto.builder().results(new ArrayList<>(List.of(failed(tc)))).build());

        new HistoryTab().load(getProject(), tab, Optional.of(tc));
        Await.until("the bug cards were never drawn: " + Drawn.words(tab), () -> Drawn.holds(Drawn.words(tab), Bundle.message("view.history.not.under.git")));

        final @NotNull List<String> words = Drawn.words(tab);
        for (final String shown : List.of(Bundle.message("view.history.bug.recorded"), Bundle.message("view.history.bug.in"), BugSeverity.MAJOR.getLabel(), BugPriority.HIGH.getLabel(), "#412", Bundle.message("view.history.not.committed"))) {
            assertTrue("a bug card does not show " + shown + ": " + words, Drawn.holds(words, shown));
        }
        assertFalse("a bug card shows the actual result: " + words, Drawn.holds(words, "The basket was emptied"));
        assertEquals("one bug card per test run, then the line: " + words, 2, words.stream().filter(Bundle.message("view.history.bug.recorded")::equals).count());

        final @NotNull JBLabel gone = (JBLabel) Drawn.reading(tab, "Cycle-0");
        assertTrue("a test run no longer in the test project does not say so: " + gone.getToolTipText(), String.valueOf(gone.getToolTipText()).contains(Bundle.message("view.history.bug.test.run.gone")));

        final @NotNull TestinEditors editors = Services.getInstance(getProject(), TestinEditors.class);
        try {
            ((ActionLink) Drawn.reading(tab, tr.getName())).doClick();
            Await.until("the test run did not open", () -> editors.editorFor(tr).filter(TestRunEditor.class::isInstance).isPresent());
            final @NotNull TestRunEditor opened = (TestRunEditor) editors.editorFor(tr).orElseThrow();
            Await.until("the run item was not selected in its test run", () -> Optional.ofNullable(opened.getList().getSelectedValue()).map(TestCaseDto::getId).filter(tc.getId()::equals).isPresent());
        } finally {
            editors.closeAll();
        }
    }

    // UC-VIEW-PANEL-007
    public void testNoTestCaseShownDrawsNothing() {
        new HistoryTab().load(getProject(), tab, Optional.empty());

        assertEquals(List.of(), Drawn.words(tab));
    }

    // Rule-VIEW-PANEL-099, Rule-VIEW-PANEL-100
    public void testATestProjectNotUnderGitSaysSoInOneLineWithNoReadingLineFirst() {
        new HistoryTab().load(getProject(), tab, Optional.of(aTestCase()));

        assertEquals("a read that has not yet taken 0.3 seconds shows a Reading line", List.of(), Drawn.words(tab));
        Await.until("the History tab never said the test project is not under Git: " + Drawn.words(tab),
                () -> Drawn.words(tab).equals(List.of(Bundle.message("view.history.not.under.git"))));
    }

    // Rule-VIEW-PANEL-099
    public void testAnAnswerForATestCaseNoLongerShownIsNeverDrawn() {
        final @NotNull HistoryTab history = new HistoryTab();
        history.load(getProject(), tab, Optional.of(aTestCase()));
        history.load(getProject(), tab, Optional.empty());

        for (int turn = 0; turn < 25; turn++) {
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
            TimeoutUtil.sleep(20);
        }

        assertEquals("the history of a test case paged away from was drawn", List.of(), Drawn.words(tab));
    }
}
