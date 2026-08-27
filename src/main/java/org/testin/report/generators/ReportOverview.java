package org.testin.report.generators;

import org.testin.model.RunEditorAttributes;
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

        // Every configuration field, walked rather than listed. Listed, it
        // printed five of the eight: the language, the browser and the device
        // type were asked of the tester, written to two files, and appeared in
        // no report at all - and a ninth question would have been the fourth.
        //
        // Only what the tester answered. A row saying nothing is a row the
        // reader has to look at to find out it says nothing.
        for (final TestRunConfiguration field : TestRunConfiguration.values()) {
            // The commit id is the one field whose blank is worth a row: it says
            // whether the run is pinned to a commit, and an empty cell reads as
            // a question the report forgot to answer.
            if (field == TestRunConfiguration.COMMIT_ID) {
                rows.add(new DetailRow(field.getDisplayName(),
                        field.valueIn(tr).isEmpty() ? NOT_RECORDED : field.valueIn(tr)));
                continue;
            }

            // The platform and the component are one row, captioned by whichever
            // halves were answered - so a run with no platform reads
            // "Component: Backend" rather than "Platform, Component: , Backend".
            if (field == TestRunConfiguration.COMPONENT) continue;

            if (field == TestRunConfiguration.PLATFORM) {
                final @NotNull String platform = TestRunConfiguration.PLATFORM.valueIn(tr);
                final @NotNull String component = TestRunConfiguration.COMPONENT.valueIn(tr);

                add(rows, ReportText.joined(", ",
                                platform.isEmpty() ? "" : TestRunConfiguration.PLATFORM.getDisplayName(),
                                component.isEmpty() ? "" : TestRunConfiguration.COMPONENT.getDisplayName()),
                        ReportText.joined(", ", platform, component));
                continue;
            }

            add(rows, field.getDisplayName(), field.valueIn(tr));
        }

        rows.add(new DetailRow(RunEditorAttributes.EXECUTED_BY.getName(), summary.executedBy()));
        rows.addAll(TestRunExecution.rowsOf(tr));
        rows.add(new DetailRow(RunEditorAttributes.RUN_STATUS.getName(), trDir.getMarker().getStatus().getLabel()));

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
