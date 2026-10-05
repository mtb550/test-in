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

package org.testin.testrun;

import com.intellij.openapi.application.WriteAction;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.TestCaseDto;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RunItemStatusPerTestRunIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull UUID testCaseId = UUID.randomUUID();

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunOver(final @NotNull TestProjectDirectoryDto tp, final @NotNull String named) {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull TestRunDirectoryDto testRun = Services.getInstance(getProject(), DirectoryMapper.class).setTestRunNode(tp.getTestRunsDirectory().getPath().resolve(named), tp.getTestRunsDirectory());
            Services.getInstance(getProject(), Nodes.class).addTestRunDir(testRun);
            return testRun.getPath();
        });

        indexedTestRuns().putTestRun(testRunPath, new TestRunDto().setResults(new ArrayList<>(List.of(new TestRunItems().setId(testCaseId)))));
        return testRunPath;
    }

    private @NotNull RunItemStatus statusIn(final @NotNull Path testRunPath) {
        return indexedTestRuns().getTestRunByPath(testRunPath).resultOf(testCaseId).orElseThrow().getStatus();
    }

    // Rule-PRODUCT-009
    public void testATestCaseCarriesItsOwnRunItemStatusInEachTestRun() {
        final @NotNull TestProjectDirectoryDto tp = WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectDirectoryDto project = Services.getInstance(getProject(), DirectoryMapper.class).setTestProjectNode(root.resolve("NAFATH"));
            Services.getInstance(getProject(), Nodes.class).addTestProject(project);
            return project;
        });
        final @NotNull Path cycle1 = aTestRunOver(tp, "Cycle-1");
        final @NotNull Path cycle2 = aTestRunOver(tp, "Cycle-2");

        indexedTestRuns().changeResult(cycle1, testCaseId, result -> result.recordRunItemStatus(RunItemStatus.FAILED, "Sara", TestCaseDto.builder().id(testCaseId).build()));
        indexedTestRuns().changeResult(cycle2, testCaseId, result -> result.recordRunItemStatus(RunItemStatus.PASSED, "Omar", TestCaseDto.builder().id(testCaseId).build()));

        assertEquals(RunItemStatus.FAILED, statusIn(cycle1));
        assertEquals("passing the test case in one test run changed what another recorded", RunItemStatus.PASSED, statusIn(cycle2));
    }
}
