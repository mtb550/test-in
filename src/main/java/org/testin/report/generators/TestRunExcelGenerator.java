package org.testin.report.generators;

import org.testin.model.RunEditorAttributes;
import com.intellij.openapi.project.Project;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunSummary;
import org.testin.logger.Logger;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestRunExcelGenerator {

    /**
     * What a spreadsheet cell shows for a description or expected result that is
     * not there - because the case is gone, or because nobody filled it in. Both
     * read the same to whoever opens the report.
     */
    private static @NotNull String orNotAvailable(final @NotNull String value) {
        return value.isEmpty() ? "N/A" : value;
    }


    /**
     * The project is not read here and is part of the signature anyway: all four
     * generators are called through one functional interface in {@code FileTypes},
     * and the three that render a document do need it (#61).
     */
    @SuppressWarnings("unused")
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            final @NotNull Workbook wb = new Workbook(os, Bundle.getPluginName(), "1.0");
            final @NotNull Worksheet ws = wb.newWorksheet("Test Run Report");

            ws.value(0, 0, "Test Run Report:");
            ws.style(0, 0).bold().fontSize(14).set();
            // The run's own name, not its change log - see the HTML generator.
            ws.value(0, 1, trDir.getName());

            ws.value(1, 0, "Platform:");
            ws.style(1, 0).bold().set();
            ws.value(1, 1, tr.getPlatform());

            ws.value(2, 0, "Status:");
            ws.style(2, 0).bold().set();
            ws.value(2, 1, trDir.getMarker().getStatus().getLabel());

            // The same headline the other three formats print, from the same
            // summary, so a spreadsheet and a PDF of one run cannot disagree.
            final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());
            final @NotNull List<String> headings = new ArrayList<>(
                    List.of(TestStatus.PASSED.getLabel(), TestStatus.FAILED.getLabel(),
                            TestStatus.BLOCKED.getLabel(), TestStatus.UNTESTED.getLabel(), "Executed", "Pass Rate"));
            final @NotNull List<String> values = new ArrayList<>(List.of(
                    String.valueOf(summary.passed()), String.valueOf(summary.failed()),
                    String.valueOf(summary.blocked()), String.valueOf(summary.untested()),
                    String.valueOf(summary.executed()), summary.passRate() + "%"));

            // Beside Untested, so the spreadsheet headline names what its own
            // rows below already carry - a removed case is in the sheet either
            // way, and a headline that skips it explains nothing.
            if (summary.hasRemoved()) {
                headings.add(4, TestStatus.REMOVED.getLabel());
                values.add(4, String.valueOf(summary.removed()));
            }

            for (int col = 0; col < headings.size(); col++) {
                ws.value(4, col, headings.get(col));
                ws.style(4, col).bold().set();
                ws.value(5, col, values.get(col));
            }

            int row = 7;
            ws.value(row, 0, "Test Case ID");
            ws.value(row, 1, RunEditorAttributes.DESCRIPTION.getName());
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
                final @NotNull UUID id = result.getId();
                ws.value(row, 0, id.toString());

                final @NotNull TestCaseDto details = ReportedCase.of(detailsMap, id);
                final @NotNull String title = orNotAvailable(details.getDescription());
                final @NotNull String expectedResult = orNotAvailable(details.getExpectedResult());

                ws.value(row, 1, title);

                final @NotNull TestStatus statusEnum = result.getStatus();
                ws.value(row, 2, statusEnum.name());
                ws.style(row, 2).fontColor(statusEnum.getHex()).bold().set();

                ws.value(row, 3, result.getActualResult());
                ws.style(row, 3).wrapText(true).set();

                ws.value(row, 4, result.getBugSeverity().getName());
                ws.style(row, 4).bold().set();

                ws.value(row, 5, result.getBugPriority().getName());
                ws.style(row, 5).bold().set();

                final @NotNull String formattedDuration = Display.formatDuration(result.getDuration());
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