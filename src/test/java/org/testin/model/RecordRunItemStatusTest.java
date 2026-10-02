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

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RecordRunItemStatusTest {

    private static final String ISSUE = "https://github.com/mtb550/product/issues/123";

    private static @NotNull TestRunItems failedWithBug() {
        return TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(RunItemStatus.FAILED)
                .bugSeverity(BugSeverity.MAJOR)
                .bugPriority(BugPriority.HIGH)
                .actualResult("NPE on the login button")
                .stacktrace("java.lang.NullPointerException at Login.click(Login.java:42)")
                .screenshots(List.of("k3f9a.png"))
                .bugIssueUrl(ISSUE)
                .build();
    }

    // Rule-VIEW-PANEL-032
    @Test
    public void passingAFailedTestCaseClearsEverythingTheFailureDescribed() {
        final TestRunItems item = failedWithBug();

        item.recordRunItemStatus(RunItemStatus.PASSED, "tester", new TestCaseDto());

        assertEquals(item.getStatus(), RunItemStatus.PASSED);
        assertEquals(item.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(item.getBugPriority(), BugPriority.LOW);
        assertEquals(item.getActualResult(), "", "the failure text describes a failure that no longer exists");
        assertEquals(item.getStacktrace(), "", "likewise the stacktrace");
        assertTrue(item.getScreenshots().isEmpty(), "and the screenshots pasted with it (#50)");
        assertEquals(item.getBugIssueUrl(), "", "and the bug it was reported as: a later failure can be reported again (#28)");
    }

    @Test
    public void failingAgainKeepsTheBugTheDialogJustCollected() {
        final TestRunItems item = failedWithBug();

        item.recordRunItemStatus(RunItemStatus.FAILED, "tester", new TestCaseDto());

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR, "re-failing must not wipe the details");
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
        assertEquals(item.getActualResult(), "NPE on the login button");
        assertEquals(item.getScreenshots().size(), 1);
        assertEquals(item.getBugIssueUrl(), ISSUE);
    }

    @Test
    public void passingATestCaseThatNeverFailedChangesNothingElse() {
        final TestRunItems item = TestRunItems.builder()
                .id(UUID.randomUUID())
                .status(RunItemStatus.PENDING)
                .build();

        item.recordRunItemStatus(RunItemStatus.PASSED, "tester", new TestCaseDto());

        assertEquals(item.getStatus(), RunItemStatus.PASSED);
        assertEquals(item.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(item.getBugPriority(), BugPriority.LOW);
    }

    // Rule-PRODUCT-010
    @Test
    public void everyRunItemStatusRecordsWhoAndWhen() {
        final TestRunItems item = failedWithBug();

        item.recordRunItemStatus(RunItemStatus.BLOCKED, "muteb", new TestCaseDto());

        assertEquals(item.getExecutedBy(), "muteb");
        assertEquals(item.getExecutedAt().getNano(), 0, "stamped to the second, as the test run JSON stores it");
    }

    @Test
    public void aTestCaseBlockedInBetweenStillClearsWhenItFinallyPasses() {
        final TestRunItems item = failedWithBug();

        item.recordRunItemStatus(RunItemStatus.BLOCKED, "tester", new TestCaseDto());
        item.recordRunItemStatus(RunItemStatus.PASSED, "tester", new TestCaseDto());

        assertEquals(item.getBugSeverity(), BugSeverity.ENHANCEMENT);
        assertEquals(item.getBugPriority(), BugPriority.LOW);
        assertEquals(item.getActualResult(), "");
        assertEquals(item.getStacktrace(), "");
        assertEquals(item.getBugIssueUrl(), "");
    }

    @Test
    public void blockingAFailedTestCaseKeepsTheDetails() {
        final TestRunItems item = failedWithBug();

        item.recordRunItemStatus(RunItemStatus.BLOCKED, "tester", new TestCaseDto());

        assertEquals(item.getBugSeverity(), BugSeverity.MAJOR);
        assertEquals(item.getBugPriority(), BugPriority.HIGH);
        assertEquals(item.getActualResult(), "NPE on the login button");
        assertFalse(item.getStacktrace().isEmpty());
        assertEquals(item.getBugIssueUrl(), ISSUE);
    }

    @Test
    public void whatEachRunItemStatusWouldClearIsNamedBeforeItClears() {
        final TestRunItems item = failedWithBug();

        assertEquals(item.wouldClear(RunItemStatus.PASSED, Failure.NONE),
                List.of("the actual result", "the stacktrace", "the screenshots", "the bug severity", "the bug priority", "the bug issue link"));
        assertEquals(item.wouldClear(RunItemStatus.FAILED, new Failure("boom", "at Login.click")),
                List.of("the actual result", "the stacktrace", "the screenshots"));
        assertEquals(item.wouldClear(RunItemStatus.FAILED, Failure.NONE), List.of(), "the keyboard's F asks nothing");
    }

    @Test
    public void everyFailureRecordsABug() {
        final TestRunItems failed = TestRunItems.builder().id(UUID.randomUUID()).status(RunItemStatus.FAILED).build();
        final TestRunItems blocked = TestRunItems.builder().id(UUID.randomUUID()).status(RunItemStatus.BLOCKED).build();

        assertTrue(FailureDetail.recordsABug(failed), "a failure is Enhancement / Low until the tester says otherwise");
        assertFalse(FailureDetail.recordsABug(blocked), "a result that is not a failure records no bug");
        assertTrue(FailureDetail.recordsABug(blocked.setBugIssueUrl(ISSUE)), "unless an issue was filed for it");
    }
}
