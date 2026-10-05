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
package org.testin.view.bugs;

import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.indexer.TestRuns;
import org.testin.model.bug.BugPriority;
import org.testin.model.bug.BugSeverity;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestCaseDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.view.AbstractViewPanelIdeTest;
import org.testin.view.Drawn;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public class OpenBugsFromMemoryIdeTest extends AbstractViewPanelIdeTest {

    // Rule-VIEW-PANEL-065
    public void testTheBugsAreReadFromTheTestRunsTestinHoldsAndNotFromDisk() {
        final @NotNull TestCaseDto tc = aTestCase(aTestSet("Login"), "Log in with a valid user", "a");
        final @NotNull TestRunDirectoryDto tr = aTestRun(List.of(TestRunItems.builder().id(tc.getId()).status(RunItemStatus.FAILED).bugSeverity(BugSeverity.MAJOR).bugPriority(BugPriority.HIGH).actualResult("The session was dropped").build()));
        Services.getInstance(getProject(), TestRuns.class).awaitWrites();

        TempTree.delete(tr.getPath());
        assertFalse("the test run is still on disk", Files.exists(tr.getPath()));

        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new OpenBugsTab().load(getProject(), tab, Optional.of(tc));

        final @NotNull List<String> words = Drawn.words(tab);
        assertTrue("the bug Testin holds in memory was not listed: " + words, words.contains(tr.getName()));
        assertTrue("the bug Testin holds in memory was not listed: " + words, words.contains("The session was dropped"));
    }
}
