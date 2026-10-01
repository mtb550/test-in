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
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.testin.TempTree;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class FailureFormSavesToTheRunIdeTest extends BasePlatformTestCase {
    private static final @NotNull UUID FAILED_TEST_CASE = UUID.fromString("33333333-3333-4333-8333-333333333301");
    private static final @NotNull String TYPED = "The expired card was accepted";

    private Path root;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        root = Files.createTempDirectory("testin-failure-form");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            TempTree.delete(root);
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aRunWithOneFailure() {
        final @NotNull Path runPath = WriteAction.computeAndWait(() -> {
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

        indexedTestRuns().putTestRun(runPath, new TestRunDto().setResults(List.of(
                new TestRunItems().setId(FAILED_TEST_CASE).setStatus(TestStatus.FAILED))));
        return runPath;
    }

    // Rule-EDITOR-PANEL-256
    public void testTheFormWritesTheFailureOntoTheRunTheIndexHolds() {
        final @NotNull Path runPath = aRunWithOneFailure();
        assertTrue("the run was not indexed", indexedTestRuns().findTestRun(runPath).isPresent());
        assertTrue("the run does not cover the test case", indexedTestRuns().getTestRunByPath(runPath).resultOf(FAILED_TEST_CASE).isPresent());

        final @NotNull TestRunItems editorsOwnRow = new TestRunItems().setId(FAILED_TEST_CASE).setStatus(TestStatus.FAILED).setActualResult(TYPED);

        final @NotNull FailureForm form = new FailureForm(getProject(), runPath, editorsOwnRow, 1f, () -> {
        }, () -> {
        });

        assertTrue("the form refused to save a failure the run covers", form.save());
        assertEquals("the failure was written somewhere other than the run the index holds", TYPED,
                indexedTestRuns().getTestRunByPath(runPath).resultOf(FAILED_TEST_CASE).map(TestRunItems::getActualResult).orElse(""));
    }
}
