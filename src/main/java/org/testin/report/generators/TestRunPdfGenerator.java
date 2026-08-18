package org.testin.report.generators;

import com.intellij.openapi.project.Project;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.ExplorerPanel;
import org.testin.logger.Logger;
import org.testin.model.*;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class TestRunPdfGenerator {


    private static final Map<Integer, float[]> COLUMN_WIDTHS = Map.of(
            0, new float[]{7, 93},
            1, new float[]{7, 83, 10},
            2, new float[]{7, 73, 10, 10}
    );
    private final DeviceRgb DARK_NAVY = new DeviceRgb(0x1F, 0x38, 0x64);
    private final DeviceRgb MEDIUM_BLUE = new DeviceRgb(0x2E, 0x54, 0x96);
    private final DeviceRgb DARK_GRAY = new DeviceRgb(0x59, 0x59, 0x59);
    private final DeviceRgb GREEN = new DeviceRgb(0x2E, 0x7D, 0x32);
    private final DeviceRgb RED = new DeviceRgb(0xC0, 0x39, 0x2B);
    private final DeviceRgb DARK_YELLOW = new DeviceRgb(0xB8, 0x86, 0x0B);
    private final DeviceRgb LIGHT_BG = new DeviceRgb(0xF2, 0xF5, 0xFA);
    private final DeviceRgb BORDER_GRAY = new DeviceRgb(0xD0, 0xD7, 0xE5);
    private final DeviceRgb WHITE = new DeviceRgb(0xFF, 0xFF, 0xFF);
    private final DeviceRgb BLACK = new DeviceRgb(0x00, 0x00, 0x00);
    private final DeviceRgb LINK_BLUE = new DeviceRgb(0x00, 0x52, 0xCC);
    private final Map<BugPriority, DeviceRgb> PRIORITY_COLOR = Map.of(
            BugPriority.HIGH, RED,
            BugPriority.MEDIUM, DARK_YELLOW
    );
    private final Map<BugSeverity, DeviceRgb> SEVERITY_COLOR = Map.of(
            BugSeverity.BLOCKER, RED,
            BugSeverity.MAJOR, DARK_YELLOW
    );

    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                                     final @NotNull TestRunDto tr,
                                     final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        // try-with-resources: closing the Document also closes the PdfDocument and
        // PdfWriter, including on any failure path inside the body.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(new PdfDocument(new PdfWriter(baos)))) {
            PdfDocument pdf = document.getPdfDocument();

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            String projectName = "";
            TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto) Services.getInstance(p, ExplorerPanel.class).getTestProjectSelector().getSelectedTestProject().getSelectedItem();

            if (selectedProject != null) {
                projectName = selectedProject.getName();
            }

            // TITLE
            document.add(new Paragraph("TEST SUMMARY REPORT")
                    .setFont(boldFont).setFontSize(18).setFontColor(DARK_NAVY)
                    .setMarginBottom(2));

            // SUBTITLE
            Paragraph subtitle = new Paragraph(projectName + "  |  " + tr.getPlatform() + ", " + tr.getComponent())
                    .setFont(regularFont).setFontSize(10).setFontColor(MEDIUM_BLUE)
                    .setPaddingBottom(4)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 2f))
                    .setMarginBottom(1);

            document.add(subtitle);


            document.add(new Paragraph("Confidential — QA Test Execution Summary")
                    .setFont(italicFont).setFontSize(8).setFontColor(DARK_GRAY)
                    .setMarginBottom(20));

            // SECTION 1: REPORT OVERVIEW
            Paragraph sec1 = new Paragraph("1. Report Overview")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(12);
            document.add(sec1);


            // One traversal of the results serves the whole report: the counts
            // below, the pass rate, and who executed it.
            final TestRunSummary summary = TestRunSummary.of(tr.getResults());

            Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            addOverviewRow(overviewTable, "Project", projectName, boldFont, regularFont);

            if (!tr.getChangeLog().isEmpty())
                addOverviewRow(overviewTable, TestRunConfiguration.CHANGE_LOG.getDisplayName(), tr.getChangeLog(), boldFont, regularFont);

            addOverviewRow(overviewTable, TestRunConfiguration.COMMIT_ID.getDisplayName(), tr.getCommitId().isEmpty() ? "n/a" : tr.getCommitId(), boldFont, regularFont);

            if (!tr.getPlatform().isEmpty() || !tr.getComponent().isEmpty())
                addOverviewRow(overviewTable, TestRunConfiguration.PLATFORM.getDisplayName() + ", " + TestRunConfiguration.COMPONENT.getDisplayName(), tr.getPlatform() + ", " + tr.getComponent(), boldFont, regularFont);

            if (!tr.getTestType().isEmpty())
                addOverviewRow(overviewTable, TestRunConfiguration.TEST_TYPE.getDisplayName(), tr.getTestType(), boldFont, regularFont);


            addOverviewRow(overviewTable, "Executed By", summary.executedBy(), boldFont, regularFont);
            addOverviewRow(overviewTable, "Execution Started", Config.formatOrBlank(tr.getExecutionStartedAt()), boldFont, regularFont);
            addOverviewRow(overviewTable, "Execution Ended", Config.formatOrBlank(tr.getExecutionEndedAt()), boldFont, regularFont);
            addOverviewRow(overviewTable, "Run Status", trDir.getMarker().getStatus().name(), boldFont, regularFont);

            document.add(overviewTable);

            // SECTION 2: EXECUTION SUMMARY
            Paragraph sec2 = new Paragraph("2. Execution Summary")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(9)
                    .setMarginTop(20);
            document.add(sec2);

            document.add(new Paragraph(
                    String.format("This run holds %d test cases, of which %d were executed. Of those, %d%% passed. The results below summarize the outcome.",
                            summary.total(), summary.executed(), summary.passRate()))
                    .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                    .setMarginBottom(12));


            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{100f / 6, 100f / 6, 100f / 6, 100f / 6, 100f / 6, 100f / 6}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            addStatCell(statsTable, String.valueOf(summary.total()), "Total Cases", DARK_NAVY, boldFont);
            addStatCell(statsTable, String.valueOf(summary.passed()), "Passed", GREEN, boldFont);
            addStatCell(statsTable, String.valueOf(summary.failed()), "Failed", RED, boldFont);
            addStatCell(statsTable, String.valueOf(summary.blocked()), "Blocked", DARK_YELLOW, boldFont);
            addStatCell(statsTable, String.valueOf(summary.untested()), "Untested", DARK_GRAY, boldFont);
            addStatCell(statsTable, summary.passRate() + "%", "Pass Rate", MEDIUM_BLUE, boldFont);

            document.add(statsTable);

            // SECTION 3: RESULT ANALYSIS
            Paragraph sec3 = new Paragraph("3. Result Analysis")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(9)
                    .setMarginTop(20);
            document.add(sec3);

            // Passed
            Paragraph passedHeading = new Paragraph()
                    .add(new Paragraph("Passed (" + summary.passed() + ")")
                            .setFont(boldFont).setFontSize(11).setFontColor(GREEN)
                            .setMarginBottom(6));
            document.add(passedHeading);

            // Failed
            Paragraph failedHeading = new Paragraph()
                    .add(new Paragraph("Failed (" + summary.failed() + ")")
                            .setFont(boldFont).setFontSize(11).setFontColor(RED)
                            .setMarginBottom(6));
            document.add(failedHeading);

            // Blocked
            Paragraph blockedHeading = new Paragraph()
                    .add(new Paragraph("Blocked (" + summary.blocked() + ")")
                            .setFont(boldFont).setFontSize(11).setFontColor(DARK_YELLOW)
                            .setMarginBottom(6));
            document.add(blockedHeading);

            // Untested
            Paragraph untestedHeading = new Paragraph()
                    .add(new Paragraph("Untested (" + summary.untested() + ")")
                            .setFont(boldFont).setFontSize(11).setFontColor(DARK_GRAY)
                            .setMarginBottom(6));
            document.add(untestedHeading);

            // SECTIONS 4+: one case table per status, empty ones omitted. Numbered
            // as printed rather than per section, so a run with nothing blocked
            // does not jump from 5 to 7.
            int sectionNumber = 4;
            for (final ReportSection section : ReportSection.values()) {
                final long count = section.count(summary);
                if (count == 0) continue;

                buildCaseTable(document, String.valueOf(sectionNumber++), section.getTitle(),
                        section.description(String.valueOf(count)), tr, detailsMap, boldFont, regularFont,
                        colorOf(section), section.isWithFailureDetail(), section::matches);
            }


            float pageWidth = pdf.getDefaultPageSize().getWidth();
            float leftMargin = document.getLeftMargin();
            float rightMargin = document.getRightMargin();

            Canvas footerCanvas = new Canvas(pdf.getLastPage(),
                    new Rectangle(leftMargin, 0, pageWidth - leftMargin - rightMargin, 28));

            // Horizontal rule above the footer text (HTML footer's border-top)
            PdfCanvas pdfCanvas = footerCanvas.getPdfCanvas();
            pdfCanvas.setStrokeColor(BORDER_GRAY);
            pdfCanvas.setLineWidth(1.0f);
            pdfCanvas.moveTo(leftMargin, 34);
            pdfCanvas.lineTo(pageWidth - rightMargin, 34);
            pdfCanvas.stroke();


            // Footer — all text on a single line:
            footerCanvas.add(new Paragraph()
                    .setFont(regularFont).setFontSize(8).setFontColor(DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Text(ZonedDateTime.now().format(Config.getDateFormatterPattern())))
                    .add(new Text("  |  Generated automatically by "))
                    .add(new Link("Testin", PdfAction.createURI("https://plugins.jetbrains.com/plugin/31514-testin"))
                            .setFontColor(LINK_BLUE))
                    .add(new Text(" IntelliJ plugin.")));

            footerCanvas.close();

            document.close();
            return baos.toByteArray();

        } catch (final Exception ex) {
            Logger.error("PDF generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }


    /**
     * The header color of a section's table. Kept here rather than on
     * {@link ReportSection} because each format has its own color type — iText
     * wants a DeviceRgb, Word a hex string — and a shared section has no business
     * knowing about either.
     */
    private @NotNull DeviceRgb colorOf(final @NotNull ReportSection section) {
        return switch (section) {
            case FAILED -> RED;
            case PASSED -> GREEN;
            case BLOCKED -> DARK_YELLOW;
            case UNTESTED -> DARK_GRAY;
        };
    }

    private void buildCaseTable(final @NotNull Document document, final @NotNull String sectionNumber,
                                final @NotNull String sectionTitle, final @NotNull String description,
                                final @NotNull TestRunDto tr,
                                final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull PdfFont boldFont,
                                final @NotNull PdfFont regularFont, final @NotNull DeviceRgb headerBg,
                                final boolean withFailureDetail, final @NotNull Predicate<TestRunItems> filter) {
        document.add(new Paragraph(sectionNumber + ". " + sectionTitle)
                .setFont(boldFont)
                .setFontSize(13)
                .setFontColor(DARK_NAVY)
                .setPaddingBottom(3)
                .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                .setMarginBottom(9)
                .setMarginTop(20));

        document.add(new Paragraph(description)
                .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                .setMarginBottom(12));

        // Column widths depend on which extra columns are shown
        int extraCols = withFailureDetail ? 2 : 0;
        float[] widths = COLUMN_WIDTHS.getOrDefault(extraCols, new float[]{7, 73, 10, 10});
        Table table = new Table(UnitValue.createPercentArray(widths))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        // Header row
        addCaseTableHeader(table, "#", headerBg, boldFont);
        addCaseTableHeader(table, "Test Case", headerBg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, "Priority", headerBg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, "Severity", headerBg, boldFont);

        // Data rows — alternating LIGHT_BG / WHITE
        int idx = 1;
        boolean alt = true;
        for (TestRunItems item : tr.getResults()) {
            if (!filter.test(item)) continue;

            DeviceRgb rowBg = alt ? LIGHT_BG : WHITE;
            alt = !alt;

            // # column
            table.addCell(new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                    .add(new Paragraph(String.valueOf(idx))
                            .setFont(regularFont).setFontSize(9.5f).setFontColor(DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)));

            // Test Case column
            final TestCaseDto tc = detailsMap.get(item.getId());
            String tcName = tc != null ? tc.getDescription() : "";
            if (tcName.isEmpty()) {
                tcName = "—";
            }
            final Cell testCaseCell = new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6);
            testCaseCell.add(new Paragraph(tcName)
                    .setFont(regularFont).setFontSize(9.5f).setFontColor(BLACK)
                    .setMarginBottom(0));
            if (withFailureDetail) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                testCaseCell.add(new Paragraph("Actual result: " + actualResult)
                        .setFont(regularFont).setFontSize(9f).setFontColor(DARK_GRAY));
            }
            table.addCell(testCaseCell);

            if (withFailureDetail) {
                BugPriority pri = item.getBugPriority();
                DeviceRgb priColor = PRIORITY_COLOR.getOrDefault(pri, DARK_GRAY);
                String priText = pri.getName();
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(new Paragraph(priText)
                                .setFont(boldFont).setFontSize(9.5f).setFontColor(priColor)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            if (withFailureDetail) {
                BugSeverity sev = item.getBugSeverity();
                DeviceRgb sevColor = SEVERITY_COLOR.getOrDefault(sev, DARK_GRAY);
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "—";
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(new Paragraph(sevText)
                                .setFont(boldFont).setFontSize(9.5f).setFontColor(sevColor)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            idx++;
        }

        document.add(table);
    }

    private void addCaseTableHeader(final @NotNull Table table, final @NotNull String text,
                                    final @NotNull DeviceRgb bgColor, final @NotNull PdfFont boldFont) {
        table.addCell(new Cell()
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(6).setPaddingRight(6)
                .add(new Paragraph(text)
                        .setFont(boldFont).setFontSize(9.5f).setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addOverviewRow(final @NotNull Table table, final @NotNull String label, final @NotNull String value,
                                final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        table.addCell(new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(new Paragraph(label)
                        .setFont(boldFont).setFontSize(10.5f).setFontColor(DARK_NAVY)));

        table.addCell(new Cell()
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(new Paragraph(value)
                        .setFont(regularFont).setFontSize(10.5f).setFontColor(BLACK)));
    }

    private void addStatCell(final @NotNull Table table, final @NotNull String number, final @NotNull String label,
                             final @NotNull DeviceRgb numberColor, final @NotNull PdfFont boldFont) {
        Cell cell = new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6);

        cell.add(new Paragraph(number)
                .setFont(boldFont).setFontSize(20).setFontColor(numberColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        cell.add(new Paragraph(label)
                .setFont(boldFont).setFontSize(9).setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        table.addCell(cell);
    }
}
