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

import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.TestRuns;
import org.testin.model.RunItemStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.view.Drawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OpenBugsTabIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull UUID TEST_CASE_ID = UUID.fromString("66666666-6666-4666-8666-666666666601");
    private static final @NotNull String ISSUE = "https://github.com/mtb550/test-in/issues/412";

    private @NotNull JBPanel<?> drawn(final @NotNull Optional<TestCaseDto> shown) {
        final @NotNull JBPanel<?> tab = new JBPanel<>();
        new OpenBugsTab().load(getProject(), tab, shown);
        return tab;
    }

    // Rule-VIEW-PANEL-038
    public void testWithNoTestCaseShownTheTabSaysToSelectOne() {
        assertEquals("the Open Bugs tab describes a test case that is not there", List.of(Bundle.message("view.bugs.no.selection")), Drawn.words(drawn(Optional.empty())));
    }

    // Rule-VIEW-PANEL-064, Rule-VIEW-PANEL-080
    public void testAFiledIssueIsALinkThatTakesNoKeyboard() {
        final @NotNull TestRunItems filed = TestRunItems.builder().id(TEST_CASE_ID).status(RunItemStatus.FAILED).bugIssueUrl(ISSUE).build();
        Services.getInstance(getProject(), TestRuns.class).putTestRun(root.resolve("NAFATH").resolve("Test Runs").resolve("Sprint 7"), TestRunDto.builder().results(new ArrayList<>(List.of(filed))).build());

        final @NotNull JBPanel<?> tab = drawn(Optional.of(TestCaseDto.builder().id(TEST_CASE_ID).build()));
        final @NotNull ActionLink issue = (ActionLink) Drawn.reading(tab, "#412");

        assertFalse("the issue link takes the keyboard", issue.isFocusable());
    }
}
