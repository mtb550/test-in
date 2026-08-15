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
 * @param untested   cases nobody ran — PENDING while the run is open, UNTESTED
 *                   once it completes or closes, which are the same fact at two
 *                   moments. A tester never sets either; the run does.
 * @param passRate   passed as a percentage of the cases that were actually run,
 *                   not of every case in the run. See {@link #executed()}.
 * @param executedBy everyone who recorded a verdict in this run, comma
 *                   separated, in the order they first appear. Read from the
 *                   run's own results, so a report says who ran the tests rather
 *                   than who printed the report.
 */
public record TestRunSummary(long total, long passed, long failed, long blocked, long untested, int passRate,
                             @NotNull String executedBy) {

    public static @NotNull TestRunSummary of(final @NotNull List<TestRunItems> results) {
        final Map<TestStatus, Long> counts = results.stream()
                .collect(Collectors.groupingBy(TestRunItems::getStatus, Collectors.counting()));

        final long passed = counts.getOrDefault(TestStatus.PASSED, 0L);
        final long failed = counts.getOrDefault(TestStatus.FAILED, 0L);
        final long blocked = counts.getOrDefault(TestStatus.BLOCKED, 0L);
        final long executed = passed + failed + blocked;

        return new TestRunSummary(
                results.size(),
                passed,
                failed,
                blocked,
                counts.getOrDefault(TestStatus.PENDING, 0L) + counts.getOrDefault(TestStatus.UNTESTED, 0L),
                executed > 0 ? (int) (passed * 100 / executed) : 0,
                whoExecuted(results));
    }

    private static @NotNull String whoExecuted(final @NotNull List<TestRunItems> results) {
        return results.stream()
                .map(TestRunItems::getExecutedBy)
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /**
     * The cases someone gave a verdict on: passed, failed or blocked. A blocked
     * case counts as run — it was attempted and something stopped it, which is a
     * result — while an untested one was never reached at all.
     * <p>
     * This is the pass rate's denominator, and the change is deliberate. It used
     * to be every case in the run, so building a hundred cases and running ten
     * of them reported a 10% pass rate even when all ten passed: a number that
     * measured how much of the run was left rather than how the tests did.
     */
    public long executed() {
        return passed + failed + blocked;
    }
}
