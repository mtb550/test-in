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

import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.TimeoutUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.editor.EditorFixtures;
import org.testin.model.TestCaseDto;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

public class HistoryTabIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull JBPanel<?> tab = new JBPanel<>(new BorderLayout());

    private @NotNull TestCaseDto aTestCase() {
        return EditorFixtures.testCase(getProject(), EditorFixtures.testSet(getProject(), EditorFixtures.testProject(getProject(), root), "Login"), "Log in with a valid user", "a");
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
