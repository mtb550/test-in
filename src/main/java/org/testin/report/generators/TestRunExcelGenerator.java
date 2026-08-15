package org.testin.report.generators;

import com.intellij.openapi.project.Project;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public final class TestRunExcelGenerator {

    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                                     final @NotNull TestRunDto tr,
                                     final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            final Workbook wb = new Workbook(os, Bundle.getPluginName(), "1.0");
            final Worksheet ws = wb.newWorksheet("Test Run Report");

            ws.value(0, 0, "Test Run Report:");
            ws.style(0, 0).bold().fontSize(14).set();
            ws.value(0, 1, tr.getChangeLog().replace(".json", ""));

            ws.value(1, 0, "Platform:");
            ws.style(1, 0).bold().set();
            ws.value(1, 1, tr.getPlatform());

            ws.value(2, 0, "Status:");
            ws.style(2, 0).bold().set();
            ws.value(2, 1, trDir.getMarker().getStatus().name());

            // The same headline the other three formats print, from the same
            // summary, so a spreadsheet and a PDF of one run cannot disagree.
            final TestRunSummary summary = TestRunSummary.of(tr.getResults());
            final String[] headings = {"Passed", "Failed", "Blocked", "Untested", "Executed", "Pass Rate"};
            final String[] values = {
                    String.valueOf(summary.passed()), String.valueOf(summary.failed()),
                    String.valueOf(summary.blocked()), String.valueOf(summary.untested()),
                    String.valueOf(summary.executed()), summary.passRate() + "%"};

            for (int col = 0; col < headings.length; col++) {
                ws.value(4, col, headings[col]);
                ws.style(4, col).bold().set();
                ws.value(5, col, values[col]);
            }

            int row = 7;
            ws.value(row, 0, "Test Case ID");
            ws.value(row, 1, "Description");
            ws.value(row, 2, "Status");
            ws.value(row, 3, "Actual Result");
            ws.value(row, 4, "Severity");
            ws.value(row, 5, "Priority");
            ws.value(row, 6, "Duration");
            ws.value(row, 7, "Expected Result");
            ws.value(row, 8, "Stacktrace");

            ws.range(row, 0, row, 8).style().bold().fillColor("E0E0E0").set();

            row++;
            for (final var result : tr.getResults()) {
                final UUID id = result.getId();
                ws.value(row, 0, id.toString());

                final TestCaseDto details = detailsMap.get(id);
                final String title = details != null ? details.getDescription() : "N/A";
                final String expectedResult = details != null ? details.getExpectedResult() : "N/A";

                ws.value(row, 1, title);

                final TestStatus statusEnum = result.getStatus();
                ws.value(row, 2, statusEnum.name());
                ws.style(row, 2).fontColor(statusEnum.getHex()).bold().set();

                ws.value(row, 3, result.getActualResult());
                ws.style(row, 3).wrapText(true).set();

                ws.value(row, 4, result.getBugSeverity().name());
                ws.style(row, 4).bold().set();

                ws.value(row, 5, result.getBugPriority().getName());
                ws.style(row, 5).bold().set();

                final String formattedDuration = Services.getInstance(p, Tools.class).getFormattedDuration(result.getDuration());
                ws.value(row, 6, formattedDuration);

                ws.value(row, 7, expectedResult);
                ws.style(row, 7).wrapText(true).set();

                ws.value(row, 8, result.getStacktrace());
                ws.style(row, 8).wrapText(true).set();

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
            ws.width(8, 60); // Stacktrace


            wb.finish();

            return os.toByteArray();
        } catch (final IOException ex) {
            Logger.error("Excel report generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
}