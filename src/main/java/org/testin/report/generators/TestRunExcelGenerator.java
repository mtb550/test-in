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

import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.markers.DetailRow;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.RunEditorAttributes;
import com.intellij.openapi.project.Project;
import org.dhatim.fastexcel.HyperLink;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugIssueUrl;
import org.testin.model.TestRunSummary;
import org.testin.logger.Logger;
import org.testin.report.ReportTile;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class TestRunExcelGenerator {

    /**
     * What a spreadsheet cell shows for a description or expected result that is
     * not there - because the case is gone, or because nobody filled it in. Both
     * read the same to whoever opens the report.
     */
    private static @NotNull String orNotAvailable(final @NotNull String value) {
        return value.isEmpty() ? Bundle.message("report.overview.not.recorded") : value;
    }


    /**
     * UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-020.
     * <p>
     * Two sheets. The first says what the other three formats say before their
     * tables - the overview, the execution summary and what the tester wrote
     * about the run. The second is the test cases alone, a header row and one
     * row each, so it can be sorted and filtered as one list.
     * <p>
     * The overview and the analysis were missing: a tester who sent the
     * spreadsheet sent a different report from the PDF of the same run, and
     * their written analysis appeared nowhere in it (#66, finding 199).
     */
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            final @NotNull Workbook wb = new Workbook(os, Bundle.getPluginName(), "1.0");
            final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());

            writeOverview(wb.newWorksheet(Bundle.message("report.excel.sheet.overview")), Services.getInstance(p, BoundTestProject.class).name(), trDir, tr, summary);
            writeCases(wb.newWorksheet(Bundle.message("report.excel.sheet.cases")), tr, detailsMap);

            wb.finish();

            return os.toByteArray();
        } catch (final IOException ex) {
            Logger.error("Excel report generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * Rule-REPORT-020.
     * <p>
     * The overview, the execution summary and the result analysis, as captions
     * and values in two columns, under the headings the other formats use.
     */
    private static void writeOverview(final @NotNull Worksheet ws, final @NotNull String projectName, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull TestRunSummary summary) {
        ws.value(0, 0, Bundle.message("report.title"));
        ws.style(0, 0).bold().fontSize(14).set();

        int row = heading(ws, 2, Bundle.message("report.heading.overview"));
        for (final DetailRow overview : ReportOverview.rowsFor(projectName, trDir, tr, summary)) {
            ws.value(row, 0, overview.caption());
            ws.style(row, 0).bold().set();
            ws.value(row++, 1, overview.value());
        }

        row = heading(ws, row + 1, Bundle.message("report.heading.execution"));
        ws.value(row++, 0, Bundle.message("report.summary.named", trDir.getName(),
                String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"));
        // A number rather than its text, so the headline can be summed, sorted
        // and charted; the unit is the cell's format, quoted so Excel prints it
        // rather than reading "%" as "times a hundred".
        for (final ReportTile figure : ReportTile.shownFor(summary)) {
            caption(ws, row, figure.getLabel(), figure.getHex());
            ws.value(row, 1, figure.amountIn(summary));
            if (!figure.getUnit().isEmpty()) ws.style(row, 1).format("0\"" + figure.getUnit() + "\"").set();
            row++;
        }

        // Only what the tester wrote - see the PDF generator.
        if (ResultAnalysis.anyWrittenIn(tr.getResultAnalysis())) {
            row = heading(ws, row + 1, Bundle.message("report.heading.analysis"));

            for (final ResultAnalysis section : ResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(tr.getResultAnalysis());
                if (written.isEmpty()) continue;

                caption(ws, row, section.heading(summary), section.getHexColor());
                ws.value(row, 1, written);
                ws.style(row++, 1).wrapText(true).set();
            }
        }

        ws.width(0, 32);
        ws.width(1, 80);
    }

    /**
     * A section heading on the overview sheet, answering the row after it.
     */
    private static int heading(final @NotNull Worksheet ws, final int row, final @NotNull String text) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontSize(12).set();

        return row + 1;
    }

    /**
     * A caption in the color the other formats print it in.
     */
    private static void caption(final @NotNull Worksheet ws, final int row, final @NotNull String text, final @NotNull String hex) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontColor(hex).set();
    }

    /**
     * Rule-REPORT-020.
     * <p>
     * The test cases: the column names, then one row per case, filled with the
     * color its verdict's table carries in the other formats.
     */
    private static void writeCases(final @NotNull Worksheet ws, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        ws.value(0, 0, Bundle.message("report.excel.caption.id"));
        ws.value(0, 1, RunEditorAttributes.DESCRIPTION.getName());
        ws.value(0, 2, RunEditorAttributes.RUN_STATUS.getName());
        ws.value(0, 3, RunEditorAttributes.ACTUAL_RESULT.getName());
        ws.value(0, 4, RunEditorAttributes.BUG_SEVERITY.getName());
        ws.value(0, 5, RunEditorAttributes.BUG_PRIORITY.getName());
        ws.value(0, 6, RunEditorAttributes.DURATION.getName());
        ws.value(0, 7, RunEditorAttributes.EXPECTED_RESULT.getName());
        ws.value(0, 8, RunEditorAttributes.BUG_ISSUE.getName());
        ws.range(0, 0, 0, 8).style().bold().fillColor("E0E0E0").set();

        int row = 1;
        for (final TestRunItems result : tr.getResults()) {
            final @NotNull UUID id = result.getId();
            final @NotNull TestCaseDto details = ReportedCase.of(detailsMap, id);

            ws.value(row, 0, id.toString());
            ws.value(row, 1, orNotAvailable(details.getDescription()));
            ws.value(row, 2, result.shownStatus().getLabel());
            ws.value(row, 3, result.getActualResult());
            ws.value(row, 4, result.getBugSeverity().getLabel());
            ws.value(row, 5, result.getBugPriority().getLabel());
            ws.value(row, 6, Display.formatDuration(result.getDuration()));
            ws.value(row, 7, orNotAvailable(details.getExpectedResult()));

            // One style call per cell: a second call on a cell replaces its font,
            // which would take the ink back off the fill.
            final @NotNull ReportSection verdict = ReportSection.of(result);
            ws.range(row, 0, row, 8).style().fillColor(verdict.getHexColor()).fontColor(verdict.textHex()).wrapText(true).set();

            // Its own column, empty when the run item was not reported (#50).
            final int line = row;
            result.bugIssue().ifPresent(url -> {
                ws.hyperlink(line, 8, HyperLink.external(url, BugIssueUrl.shortReference(url)));
                ws.style(line, 8).fillColor(verdict.getHexColor()).fontColor(verdict.textHex()).underlined().set();
            });

            row++;
        }

        ws.width(0, 40); // ID
        ws.width(1, 30); // Title
        ws.width(2, 15); // Status
        ws.width(3, 30); // Actual Result
        ws.width(4, 15); // Severity
        ws.width(5, 15); // Priority
        ws.width(6, 15); // Duration
        ws.width(7, 40); // Expected Result
        ws.width(8, 15); // Bug Issue
    }
}