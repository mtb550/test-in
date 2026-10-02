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

package org.testin.model;

import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRunCompletionTest {

    private static @NotNull TestRunItems item(final RunItemStatus status) {
        return TestRunItems.builder().id(UUID.randomUUID()).status(status).build();
    }

    private static @NotNull TestRunDto testRunOf(final TestRunItems... items) {
        return TestRunDto.builder().results(List.of(items)).build();
    }

    @Test
    public void everyTestCaseJudgedMeansTheTestRunIsOver() {
        assertTrue(testRunOf(item(RunItemStatus.PASSED), item(RunItemStatus.FAILED), item(RunItemStatus.BLOCKED)).isFullyJudged());
    }

    @Test
    public void oneTestCaseStillPendingKeepsItOpen() {
        assertFalse(testRunOf(item(RunItemStatus.PASSED), item(RunItemStatus.PENDING)).isFullyJudged(),
                "the test run is still expecting something about that test case");
    }

    @Test
    public void soDoesOneUntested() {
        assertFalse(testRunOf(item(RunItemStatus.PASSED), item(RunItemStatus.UNTESTED)).isFullyJudged());
    }

    @Test
    public void aDeletedTestCaseDoesNotHoldItOpenForever() {
        assertTrue(testRunOf(item(RunItemStatus.PASSED), item(RunItemStatus.REMOVED)).isFullyJudged());
    }

    @Test
    public void aTestRunWithNoTestCasesIsEmptyRatherThanFinished() {
        assertFalse(testRunOf().isFullyJudged(),
                "completing a test run the moment it is created is the wrong answer to a question nobody asked");
    }
}
