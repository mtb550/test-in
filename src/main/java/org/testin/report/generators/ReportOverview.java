package org.testin.report.generators;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunExecution;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.DetailRow;

import java.util.ArrayList;
import java.util.List;

/**
 * What section one of every report says about the run.
 * <p>
 * The three generators each held their own copy of these ten rows - the same
 * captions, the same values, the same four conditions - and they had already
 * drifted apart in four ways at once: the HTML report was missing the change
 * log, the commit id and the component, and printed a Test Type of "API
 * Functional Testing" that it had never read from the run; the Word file
 * captioned the platform pair with a backslash where the other two used a
 * comma; and the run status came out as a word in one format and as an enum
 * constant in the other two.
 * <p>
 * None of that was catchable, because three files agreeing is not something a
 * compiler can check. Stated once here, it is not something that has to be
 * checked.
 * <p>
 * Rows, not rendering: a caption and a value are all three formats have in
 * common, and iText, Word and a stylesheet have nothing else worth sharing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReportOverview {

    /**
     * The value a report prints for a commit nobody recorded. Not blank: the row
     * is there to say whether this run is pinned to a commit, and an empty cell
     * reads as a question the report forgot to answer.
     */
    private static final @NotNull String NOT_RECORDED = "n/a";

    public static @NotNull List<DetailRow> rowsFor(final @NotNull String projectName, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull TestRunSummary summary) {
        final @NotNull List<DetailRow> rows = new ArrayList<>();

        rows.add(new DetailRow("Project", projectName));
        rows.add(new DetailRow("Test Run", trDir.getName()));

        // The four that are only printed when the tester answered them. A row
        // saying nothing is a row the reader has to look at to find out it says
        // nothing.
        add(rows, TestRunConfiguration.CHANGE_LOG.getDisplayName(), tr.getChangeLog());

        rows.add(new DetailRow(TestRunConfiguration.COMMIT_ID.getDisplayName(),
                tr.getCommitId().isEmpty() ? NOT_RECORDED : tr.getCommitId()));

        // Whichever of the pair was answered, captioned by the same halves - so a
        // run with no platform reads "Component: Backend" rather than
        // "Platform, Component: , Backend".
        add(rows, ReportText.joined(", ",
                        tr.getPlatform().isEmpty() ? "" : TestRunConfiguration.PLATFORM.getDisplayName(),
                        tr.getComponent().isEmpty() ? "" : TestRunConfiguration.COMPONENT.getDisplayName()),
                ReportText.joined(", ", tr.getPlatform(), tr.getComponent()));

        add(rows, TestRunConfiguration.TEST_TYPE.getDisplayName(), tr.getTestType());

        rows.add(new DetailRow("Executed By", summary.executedBy()));
        rows.addAll(TestRunExecution.rowsOf(tr));
        rows.add(new DetailRow("Run Status", trDir.getMarker().getStatus().getLabel()));

        return List.copyOf(rows);
    }

    /**
     * A row, unless there is nothing to put in it.
     */
    private static void add(final @NotNull List<DetailRow> rows, final @NotNull String caption, final @NotNull String value) {
        if (value.isEmpty()) return;

        rows.add(new DetailRow(caption, value));
    }
}
