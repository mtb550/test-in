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
import org.testin.indexer.TestRuns;
import org.testin.model.status.RunItemStatus;
import org.testin.model.result.TestRunItems;
import org.testin.model.status.TestRunStatus;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.services.Services;
import org.testin.testrun.RunItemStatusService;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SignedOffTestRunIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String RECORDED = "The expired card was accepted";

    private final @NotNull UUID failed = UUID.randomUUID();

    private @NotNull TestRuns indexedTestRuns() {
        return Services.getInstance(getProject(), TestRuns.class);
    }

    private @NotNull Path aTestRunWithOneFailure() {
        final @NotNull Path testRunPath = WriteAction.computeAndWait(() -> {
            final @NotNull DirectoryMapper mapper = Services.getInstance(getProject(), DirectoryMapper.class);
            final @NotNull Nodes nodes = Services.getInstance(getProject(), Nodes.class);
            final @NotNull TestProjectDirectoryDto tp = mapper.setTestProjectNode(root.resolve("NAFATH"));
            nodes.addTestProject(tp);

            final @NotNull Path path = tp.getTestRunsDirectory().getPath().resolve("Cycle-1");
            nodes.addTestRunDir(mapper.setTestRunNode(path, tp.getTestRunsDirectory()));
            return path;
        });

        indexedTestRuns().putTestRun(testRunPath, new TestRunDto().setResults(List.of(
                new TestRunItems().setId(failed).setStatus(RunItemStatus.FAILED).setActualResult(RECORDED))));
        return testRunPath;
    }

    private boolean saveAFailureOnto(final @NotNull Path testRunPath) {
        final @NotNull TestRunItems row = new TestRunItems().setId(failed).setStatus(RunItemStatus.FAILED).setActualResult("Typed after the sign-off");
        return new FailureForm(getProject(), testRunPath, row, 1f, () -> {
        }, () -> {
        }).save();
    }

    // Rule-TREE-PANEL-009, Rule-PRODUCT-011
    public void testASignedOffTestRunRecordsNothingFurther() {
        final @NotNull Path testRunPath = aTestRunWithOneFailure();
        final @NotNull RunItemStatusService service = Services.getInstance(getProject(), RunItemStatusService.class);

        assertTrue("an open test run refused a write", service.heldTestRun(testRunPath).isPresent());

        for (final TestRunStatus signedOff : List.of(TestRunStatus.COMPLETED, TestRunStatus.CLOSED)) {
            indexedTestRuns().changeTestRunMarker(testRunPath, marker -> marker.setStatus(signedOff));

            assertTrue(signedOff + " still took a write", service.heldTestRun(testRunPath).isEmpty());
            assertFalse(signedOff + " saved a failure typed after the sign-off", saveAFailureOnto(testRunPath));
            assertEquals(signedOff + " changed what it recorded", RECORDED,
                    indexedTestRuns().getTestRunByPath(testRunPath).resultOf(failed).map(TestRunItems::getActualResult).orElse(""));
        }
    }
}
