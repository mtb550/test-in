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

package org.testin.report.generators;

import org.testin.model.TestRunItems;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ReportSectionTest {

    @Test
    public void everyStatusBelongsToExactlyOneSection() {
        for (final TestStatus status : TestStatus.values()) {
            final List<ReportSection> claiming = Arrays.stream(ReportSection.values())
                    .filter(section -> section.matches(item(status)))
                    .toList();

            assertEquals(claiming.size(), 1,
                    status + " is printed by " + claiming.size() + " sections, not one: " + claiming);
        }
    }

    @Test
    public void everySectionCountMatchesTheRowsItWillPrint() {
        final List<TestRunItems> results = new ArrayList<>();
        for (final TestStatus status : TestStatus.values()) {
            results.add(item(status));
            results.add(item(status));
        }

        final TestRunSummary summary = TestRunSummary.of(results);

        for (final ReportSection section : ReportSection.values()) {
            final long rows = results.stream().filter(section::matches).count();
            assertEquals(section.count(summary), rows, section + " heading and rows disagree");
        }
    }

    @Test
    public void theSectionsAccountForEveryTestCaseInTheRun() {
        final List<TestRunItems> results = List.of(
                item(TestStatus.PASSED), item(TestStatus.PASSED), item(TestStatus.FAILED),
                item(TestStatus.BLOCKED), item(TestStatus.PENDING), item(TestStatus.UNTESTED));

        final TestRunSummary summary = TestRunSummary.of(results);
        final long printed = Arrays.stream(ReportSection.values())
                .mapToLong(section -> section.count(summary))
                .sum();

        assertEquals(printed, summary.total());
    }

    @Test
    public void failuresArePrintedFirst() {
        assertEquals(ReportSection.values()[0], ReportSection.FAILED);
    }

    @Test
    public void onlyTheFailedTableCarriesFailureDetail() {
        for (final ReportSection section : ReportSection.values()) {
            assertEquals(section.isWithFailureDetail(), section == ReportSection.FAILED, section.toString());
        }
    }

    @Test
    public void theCountIsRenderedIntoTheDescription() {
        final String description = ReportSection.FAILED.description("<b>7</b>");

        assertTrue(description.contains("<b>7</b>"), description);
    }

    private TestRunItems item(final TestStatus status) {
        final TestRunItems item = new TestRunItems();
        item.setId(UUID.randomUUID());
        item.setStatus(status);
        return item;
    }
}
