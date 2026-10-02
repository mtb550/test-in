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

package org.testin.lightmode;

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunItems;
import org.testin.model.RunItemStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class FailureFormSavesToTheTestRunIdeTest extends AbstractTempRootIdeTest {
    private static final @NotNull UUID FAILED_TEST_CASE = UUID.fromString("33333333-3333-4333-8333-333333333301");
    private static final @NotNull String TYPED = "The expired card was accepted";

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunWithOneFailure() {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectDirectoryDto tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(tp);

            final @NotNull TestSetDirectoryDto ts = mapper.getTestSetNode(tp.getTestCasesDirectory().getPath().resolve("Checkout"), tp.getTestCasesDirectory());
            nodes.addTestSet(ts);
            Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(ts.getPath(),
                    TestCaseDto.builder().id(FAILED_TEST_CASE).description("Refuse an expired card").order("m").build());

            final @NotNull Path path = tp.getTestRunsDirectory().getPath().resolve("Cycle-1");
            nodes.addTestRunDir(mapper.setTestRunNode(path, tp.getTestRunsDirectory()));
            return path;
        });

        indexedTestRuns().putTestRun(testRunPath, new TestRunDto().setResults(List.of(
                new TestRunItems().setId(FAILED_TEST_CASE).setStatus(RunItemStatus.FAILED))));
        return testRunPath;
    }

    // Rule-EDITOR-PANEL-256, Rule-PRODUCT-008
    public void testTheFormWritesTheFailureOntoTheTestRunTheIndexHolds() {
        final @NotNull Path testRunPath = aTestRunWithOneFailure();
        assertTrue("the test run was not indexed", indexedTestRuns().findTestRun(testRunPath).isPresent());
        assertTrue("the test run does not cover the test case", indexedTestRuns().getTestRunByPath(testRunPath).resultOf(FAILED_TEST_CASE).isPresent());

        final @NotNull TestRunItems editorsOwnRow = new TestRunItems().setId(FAILED_TEST_CASE).setStatus(RunItemStatus.FAILED).setActualResult(TYPED);

        final @NotNull FailureForm form = new FailureForm(getProject(), testRunPath, editorsOwnRow, 1f, () -> {
        }, () -> {
        });

        assertTrue("the form refused to save a failure the test run covers", form.save());
        assertEquals("the failure was written somewhere other than the test run the index holds", TYPED,
                indexedTestRuns().getTestRunByPath(testRunPath).resultOf(FAILED_TEST_CASE).map(TestRunItems::getActualResult).orElse(""));
    }
}
