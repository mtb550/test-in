package org.testin.report.generators;

import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The one thing a reader needs from a report: every case in the run appears in
 * it, exactly once.
 * <p>
 * Both halves of that are silent when broken. A status no section claims is a
 * case that vanishes from all four formats - counted in the total at the top,
 * absent from every table below, with nothing to say where it went. A status two
 * sections claim is printed twice under headings that contradict each other.
 * Neither shows up as a failure anywhere else, which is why it is asserted here.
 */
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

    /**
     * The counts come from the summary and the rows from the filter, so a section
     * whose two halves disagree prints a heading saying "the following 3 cases"
     * above a table of one. They are separate code paths; this is what ties them.
     */
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

    /**
     * Sum of the sections is the run: nothing counted at the top is missing from
     * the tables.
     */
    @Test
    public void theSectionsAccountForEveryCaseInTheRun() {
        final List<TestRunItems> results = List.of(
                item(TestStatus.PASSED), item(TestStatus.PASSED), item(TestStatus.FAILED),
                item(TestStatus.BLOCKED), item(TestStatus.PENDING), item(TestStatus.UNTESTED));

        final TestRunSummary summary = TestRunSummary.of(results);
        final long printed = Arrays.stream(ReportSection.values())
                .mapToLong(section -> section.count(summary))
                .sum();

        assertEquals(printed, summary.total());
    }

    /**
     * Failures first. A reader who stops after one table should have read the one
     * that needs action, and all three formats number their sections from this
     * order.
     */
    @Test
    public void failuresArePrintedFirst() {
        assertEquals(ReportSection.values()[0], ReportSection.FAILED);
    }

    /**
     * Priority, severity and the actual result only say something about a case
     * that failed. On a passed row they are empty columns.
     */
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
