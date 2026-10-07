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

package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.Await;
import org.testin.NodesOnDisk;
import org.testin.model.DirectoryType;
import org.testin.model.TestRunDto;
import org.testin.model.node.TestProjectDirectoryDto;
import org.testin.model.node.TestRunDirectoryDto;
import org.testin.model.status.TestRunStatus;
import org.testin.services.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarkerNotSavedIdeTest extends AbstractTempRootIdeTest {

    // Rule-INTERNAL-123
    public void testATestRunMarkerThatCannotBeWrittenIsReadAgainFromDisk() {
        final @NotNull NodesOnDisk made = new NodesOnDisk(getProject());
        final @NotNull TestProjectDirectoryDto tp = made.testProject(root.resolve("NAFATH"));
        final @NotNull TestRunDirectoryDto testRun = made.testRun(tp.getTestRunsDirectory(), "Cycle-1");
        Services.getInstance(getProject(), TestRuns.class).putTestRun(testRun.getPath(), new TestRunDto());

        final @NotNull Path marker = testRun.getPath().resolve(DirectoryType.TR.getMarker());
        try {
            Files.delete(marker);
            Files.createDirectory(marker);
        } catch (final IOException ex) {
            throw new AssertionError("Could not put a folder where the marker was: " + ex.getMessage(), ex);
        }

        Services.getInstance(getProject(), TestRuns.class).changeTestRunMarker(testRun.getPath(), changed -> changed.setStatus(TestRunStatus.CLOSED));
        assertEquals("the change was not made in memory first", TestRunStatus.CLOSED, testRun.getMarker().getStatus());

        Await.until("a test run marker that could not be written kept its unsaved change", () -> testRun.getMarker().getStatus() != TestRunStatus.CLOSED);
    }
}
