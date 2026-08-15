package org.testin.report.generators;

import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The headline counts of a test run, computed once for every report format.
 * <p>
 * It exists because the three generators counted for themselves and drifted:
 * PDF and Word treated pending as PENDING + UNTESTED, HTML counted PENDING
 * alone. Completing a run turns every PENDING case into UNTESTED, so the HTML
 * report claimed nothing was outstanding on exactly the runs where something
 * was.
 *
 * @param pending    cases that were not executed — PENDING before a run
 *                   finishes, UNTESTED after it, which are the same fact at two
 *                   moments.
 * @param executedBy everyone who recorded a verdict in this run, comma
 *                   separated, in the order they first appear. Read from the
 *                   run's own results, so a report says who ran the tests rather
 *                   than who printed the report.
 */
public record TestRunSummary(long total, long passed, long failed, long blocked, long pending, int passRate,
                             @NotNull String executedBy) {

    public static @NotNull TestRunSummary of(final @NotNull List<TestRunItems> results) {
        final Map<TestStatus, Long> counts = results.stream()
                .collect(Collectors.groupingBy(TestRunItems::getStatus, Collectors.counting()));

        final long total = results.size();
        final long passed = counts.getOrDefault(TestStatus.PASSED, 0L);

        return new TestRunSummary(
                total,
                passed,
                counts.getOrDefault(TestStatus.FAILED, 0L),
                counts.getOrDefault(TestStatus.BLOCKED, 0L),
                counts.getOrDefault(TestStatus.PENDING, 0L) + counts.getOrDefault(TestStatus.UNTESTED, 0L),
                total > 0 ? (int) (passed * 100 / total) : 0,
                executedBy(results));
    }

    private static @NotNull String executedBy(final @NotNull List<TestRunItems> results) {
        return results.stream()
                .map(TestRunItems::getExecutedBy)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
