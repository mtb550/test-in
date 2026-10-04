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

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;
import org.testng.annotations.Test;

import java.util.Objects;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class OpenBugTest {

    private static final @NotNull UUID TEST_CASE = UUID.fromString("44444444-4444-4444-8444-444444444401");
    private static final @NotNull UUID NEVER_FAILED = UUID.fromString("44444444-4444-4444-8444-444444444402");
    private static final @NotNull String ISSUE = "https://github.com/mtb550/test-in/issues/412";
    private static final @NotNull Path CYCLE_1 = Path.of("Test Runs", "Cycle 1");
    private static final @NotNull Path CYCLE_2 = Path.of("Test Runs", "Cycle 2");
    private static final @NotNull Path CYCLE_3 = Path.of("Test Runs", "Cycle 3");
    private static final @NotNull Path CYCLE_4 = Path.of("Test Runs", "Cycle 4");

    private static @NotNull TestRunDto testRun(final @NotNull TestRunItems testCaseResult) {
        return TestRunDto.builder().results(List.of(testCaseResult, TestRunItems.builder().id(NEVER_FAILED).status(RunItemStatus.PASSED).build())).build();
    }

    private static @NotNull Map<Path, TestRunDto> fourTestRuns() {
        return Map.of(
                CYCLE_1, testRun(TestRunItems.builder().id(TEST_CASE).status(RunItemStatus.FAILED).bugSeverity(BugSeverity.MAJOR).build()),
                CYCLE_2, testRun(TestRunItems.builder().id(TEST_CASE).status(RunItemStatus.FAILED).bugSeverity(BugSeverity.MINOR).build()),
                CYCLE_3, testRun(TestRunItems.builder().id(TEST_CASE).status(RunItemStatus.PASSED).build()),
                CYCLE_4, testRun(TestRunItems.builder().id(TEST_CASE).status(RunItemStatus.PASSED).bugIssueUrl(ISSUE).build()));
    }

    // Rule-VIEW-PANEL-064
    @Test
    public void everyBugTheTestCaseRecordedIsListedWithTheTestRunThatRecordedIt() {
        final @NotNull Map<Path, OpenBug> bugs = OpenBug.of(fourTestRuns(), TEST_CASE).stream()
                .collect(Collectors.toMap(OpenBug::testRunPath, bug -> bug));

        assertEquals(bugs.keySet(), Set.of(CYCLE_1, CYCLE_2, CYCLE_4), "a test run where the test case passed with no issue recorded no bug");
        assertEquals(Objects.requireNonNull(bugs.get(CYCLE_1), "no bug for CYCLE_1").item().getBugSeverity(), BugSeverity.MAJOR, "each test run keeps its own bug");
        assertEquals(Objects.requireNonNull(bugs.get(CYCLE_2), "no bug for CYCLE_2").item().getBugSeverity(), BugSeverity.MINOR, "each test run keeps its own bug");
        assertEquals(Objects.requireNonNull(bugs.get(CYCLE_4), "no bug for CYCLE_4").item().getBugIssueUrl(), ISSUE, "a filed issue is a bug even after the test case passes");
    }

    // Rule-VIEW-PANEL-064
    @Test
    public void aTestCaseThatNeverFailedHasNoBug() {
        assertTrue(OpenBug.of(fourTestRuns(), NEVER_FAILED).isEmpty());
    }
}
