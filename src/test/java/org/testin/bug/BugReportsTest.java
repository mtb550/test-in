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

package org.testin.bug;

import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.util.Bundle;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Which run items have a bug report on the way, and why Report Bug is off
 * (#28).
 */
public class BugReportsTest {

    private static final BugReports.RunItem ITEM = new BugReports.RunItem(Path.of("NAFATH", "Test Runs", "Sprint 7"), UUID.randomUUID());
    private static final BugReports.RunItem SAME_CASE_OTHER_RUN = new BugReports.RunItem(Path.of("NAFATH", "Test Runs", "Sprint 8"), ITEM.id());

    private static TestRunItems failed() {
        return TestRunItems.builder().id(ITEM.id()).status(TestStatus.FAILED).build();
    }

    @Test
    public void reportBugSaysWhereItsOwnReportIs() {
        final BugReports reports = new BugReports();

        reports.begin(ITEM);
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.of(Bundle.message("bug.preparing")));

        reports.moveTo(ITEM, BugReports.Stage.OPEN);
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.of(Bundle.message("bug.open")));

        reports.moveTo(ITEM, BugReports.Stage.SENDING);
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.of(Bundle.message("bug.sending")),
                "while sending the run item has no link yet, so the stage is what keeps it off");

        assertFalse(reports.end(ITEM, BugReports.Stage.OPEN), "a dialog closing on Send does not end the send");
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.of(Bundle.message("bug.sending")));

        assertTrue(reports.end(ITEM, BugReports.Stage.SENDING));
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.empty());
    }

    @Test
    public void aReportedBugKeepsReportBugOff() {
        final BugReports reports = new BugReports();
        final TestRunItems reported = TestRunItems.builder().id(ITEM.id()).status(TestStatus.FAILED).bugIssueUrl("https://github.com/mtb550/test-in/issues/412").build();

        assertEquals(reports.whyReportBugIsOff(ITEM, reported), Optional.of(Bundle.message("bug.already.reported")));
    }

    @Test
    public void anOpenReportKeepsEveryOtherRunItemWaiting() {
        final BugReports reports = new BugReports();
        reports.begin(SAME_CASE_OTHER_RUN);

        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.empty(), "one being prepared, even for the same test case in another run, does not hold the others");

        reports.moveTo(SAME_CASE_OTHER_RUN, BugReports.Stage.OPEN);
        assertEquals(reports.whyReportBugIsOff(ITEM, failed()), Optional.of(Bundle.message("bug.finish.open.report")));
        assertTrue(reports.anotherIsOpen(ITEM));
        assertFalse(reports.anotherIsOpen(SAME_CASE_OTHER_RUN), "its own dialog is not another one");
    }

    @Test
    public void unsentEditsAreKeptUntilDiscarded() {
        final BugReports reports = new BugReports();
        final BugReports.Edits edits = new BugReports.Edits("Log in fails", "body");

        reports.keep(ITEM, edits);
        assertEquals(reports.unsent(ITEM), Optional.of(edits), "a failed send reopens with them");

        reports.discard(ITEM);
        assertEquals(reports.unsent(ITEM), Optional.empty());
    }
}
