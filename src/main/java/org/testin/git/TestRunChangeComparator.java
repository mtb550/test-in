package org.testin.git;

import org.testin.model.TestRunConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.TestRunExecution;
import org.testin.model.dto.TestRunDto;

import java.util.*;

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
        final @NotNull List<FieldChange> changes = new ArrayList<>();

        addIfChanged(changes, "Results", verdictSummary(oldRun), verdictSummary(newRun));
        // Walked, not listed - the eight were written out here directly above
        // a loop whose comment explains why walking is right.
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldRun), field.valueIn(newRun));
        }

        // Through the same enum the Details popup and the report read, so a
        // change reads under the heading they show it under and in the format
        // they show it in. Not as rows: this compares two revisions, and a row
        // carries one value.
        for (final TestRunExecution field : TestRunExecution.values()) {
            addIfChanged(changes, field.getDisplayName(), field.valueIn(oldRun), field.valueIn(newRun));
        }

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
        final @NotNull List<TestRunItems> results = run.getResults();
        if (results.isEmpty()) return "no cases";

        final @NotNull Map<TestStatus, Integer> counts = new EnumMap<>(TestStatus.class);
        for (final TestRunItems item : results) {
            counts.merge(item.getStatus(), 1, Integer::sum);
        }

        final @NotNull StringBuilder line = new StringBuilder(results.size() + " case" + (results.size() == 1 ? "" : "s") + ": ");
        boolean first = true;
        for (final Map.Entry<TestStatus, Integer> entry : counts.entrySet()) {
            if (!first) line.append(", ");
            line.append(entry.getValue()).append(' ').append(entry.getKey().getLabel());
            first = false;
        }

        return line.toString();
    }

    private static void addIfChanged(final @NotNull List<FieldChange> changes, final @NotNull String field, final @NotNull String oldValue, final @NotNull String newValue) {
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(new FieldChange(field, oldValue, newValue, ChangeType.CHANGE_TEST_RUN));
        }
    }
}
