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
import java.util.UUID;

public final class TestRunExcelGenerator {
    private static @NotNull String orNotAvailable(final @NotNull String value) {
        return value.isEmpty() ? Bundle.message("report.overview.not.recorded") : value;
    }

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-020
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            final @NotNull Workbook wb = new Workbook(os, Bundle.getPluginName(), "1.0");
            final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());

            writeOverview(wb.newWorksheet(Bundle.message("report.excel.sheet.overview")), Services.getInstance(p, BoundTestProject.class).name(), trDir, tr, summary);
            writeCases(wb.newWorksheet(Bundle.message("report.excel.sheet.cases")), tr);

            wb.finish();

            return os.toByteArray();
        } catch (final IOException ex) {
            Logger.error("Excel report generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    // Rule-REPORT-020
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
        for (final ReportTile figure : ReportTile.shownFor(summary)) {
            caption(ws, row, figure.getLabel(), figure.getHex());
            ws.value(row, 1, figure.amountIn(summary));
            if (!figure.getUnit().isEmpty()) ws.style(row, 1).format("0\"" + figure.getUnit() + "\"").set();
            row++;
        }

        if (ResultAnalysis.anyWrittenIn(trDir.getMarker().getResultAnalysis())) {
            row = heading(ws, row + 1, Bundle.message("report.heading.analysis"));

            for (final ResultAnalysis section : ResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(trDir.getMarker().getResultAnalysis());
                if (written.isEmpty()) continue;

                caption(ws, row, section.heading(summary), section.getHexColor());
                ws.value(row, 1, written);
                ws.style(row++, 1).wrapText(true).set();
            }
        }

        ws.width(0, 32);
        ws.width(1, 80);
    }

    private static int heading(final @NotNull Worksheet ws, final int row, final @NotNull String text) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontSize(12).set();

        return row + 1;
    }

    private static void caption(final @NotNull Worksheet ws, final int row, final @NotNull String text, final @NotNull String hex) {
        ws.value(row, 0, text);
        ws.style(row, 0).bold().fontColor(hex).set();
    }

    // Rule-REPORT-020
    private static void writeCases(final @NotNull Worksheet ws, final @NotNull TestRunDto tr) {
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
            final @NotNull TestCaseDto details = result.shownCase();

            ws.value(row, 0, id.toString());
            ws.value(row, 1, orNotAvailable(details.getDescription()));
            ws.value(row, 2, result.shownStatus().getLabel());
            ws.value(row, 3, result.getActualResult());
            ws.value(row, 4, result.getBugSeverity().getLabel());
            ws.value(row, 5, result.getBugPriority().getLabel());
            ws.value(row, 6, Display.formatDuration(result.getDuration()));
            ws.value(row, 7, orNotAvailable(details.getExpectedResult()));

            final @NotNull ReportSection verdict = ReportSection.of(result);
            ws.range(row, 0, row, 8).style().fillColor(verdict.getHexColor()).fontColor(verdict.textHex()).wrapText(true).set();

            final int line = row;
            result.bugIssue().ifPresent(url -> {
                ws.hyperlink(line, 8, HyperLink.external(url, BugIssueUrl.shortReference(url)));
                ws.style(line, 8).fillColor(verdict.getHexColor()).fontColor(verdict.textHex()).underlined().set();
            });

            row++;
        }

        ws.width(0, 40);
        ws.width(1, 30);
        ws.width(2, 15);
        ws.width(3, 30);
        ws.width(4, 15);
        ws.width(5, 15);
        ws.width(6, 15);
        ws.width(7, 40);
        ws.width(8, 15);
    }
}