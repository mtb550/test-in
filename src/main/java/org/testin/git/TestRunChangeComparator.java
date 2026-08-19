package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.util.Tools;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compares two revisions of a test run, the way {@link TestCaseChangeComparator}
 * does for a test case.
 * <p>
 * A run's rows are its results. A tester reviewing a commit wants to know what
 * happened to them, not which of forty fields moved. So the verdicts become one
 * summarized line, and the configuration fields that describe the cycle are
 * compared one by one.
 * <p>
 * Nothing here reverts. A verdict is a record of work, not an edit: putting it
 * back would say a case was never run.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class TestRunChangeComparator {

    static @NotNull List<FieldChange> compare(final @NotNull TestRunDto oldRun, final @NotNull TestRunDto newRun) {
        final List<FieldChange> changes = new ArrayList<>();

        addIfChanged(changes, "Results", verdictSummary(oldRun), verdictSummary(newRun));
        addIfChanged(changes, "Change Log", oldRun.getChangeLog(), newRun.getChangeLog());
        addIfChanged(changes, "Commit Id", oldRun.getCommitId(), newRun.getCommitId());
        addIfChanged(changes, "Platform", oldRun.getPlatform(), newRun.getPlatform());
        addIfChanged(changes, "Component", oldRun.getComponent(), newRun.getComponent());
        addIfChanged(changes, "Language", oldRun.getLanguage(), newRun.getLanguage());
        addIfChanged(changes, "Browser", oldRun.getBrowser(), newRun.getBrowser());
        addIfChanged(changes, "Device Type", oldRun.getDeviceType(), newRun.getDeviceType());
        addIfChanged(changes, "Test Type", oldRun.getTestType(), newRun.getTestType());
        addIfChanged(changes, "Execution Started", Tools.formatDate(oldRun.getExecutionStartedAt()),
                Tools.formatDate(newRun.getExecutionStartedAt()));
        addIfChanged(changes, "Execution Ended", Tools.formatDate(oldRun.getExecutionEndedAt()),
                Tools.formatDate(newRun.getExecutionEndedAt()));

        // A run file that changed with nothing above different still changed -
        // an id, a field this comparator does not read - and it has to be
        // selectable, because the commit stages only what the review lists.
        if (changes.isEmpty()) {
            changes.add(new FieldChange("Test Run", "", "changed", ChangeType.CHANGE_TEST_RUN));
        }

        return changes;
    }

    /**
     * The run's results in one line: how many cases, and how many at each
     * status that any case holds. "12 cases: 7 Passed, 3 Failed, 2 Pending".
     */
    static @NotNull String verdictSummary(final @NotNull TestRunDto run) {
        final List<TestRunItems> results = run.getResults();
        if (results.isEmpty()) return "no cases";

        final Map<TestStatus, Integer> counts = new EnumMap<>(TestStatus.class);
        for (final TestRunItems item : results) {
            counts.merge(item.getStatus(), 1, Integer::sum);
        }

        final StringBuilder line = new StringBuilder(results.size() + " case" + (results.size() == 1 ? "" : "s") + ": ");
        boolean first = true;
        for (final Map.Entry<TestStatus, Integer> entry : counts.entrySet()) {
            if (!first) line.append(", ");
            line.append(entry.getValue()).append(' ').append(entry.getKey().getLabel());
            first = false;
        }

        return line.toString();
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field,
                                     final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_TEST_RUN));
        }
    }
}
