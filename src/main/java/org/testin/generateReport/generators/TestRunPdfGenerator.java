package org.testin.generateReport.generators;

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
import org.testin.enums.BugPriority;
import org.testin.enums.BugSeverity;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.settings.AppSettingsState;
import org.testin.util.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TestRunPdfGenerator {

    // Colors from the Word template
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
    private final DeviceRgb LINK_BLUE = new DeviceRgb(0x00, 0x52, 0xCC); // #0052cc, matches HTML footer link

    public byte[] generate(final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            String cleanName = tr.getRunName().replace(".json", "");

            // Tester identity from settings (fallback to marker / literal)
            AppSettingsState settings = AppSettingsState.getInstance();
            String testerName = settings.testerName.trim();
            String testerRole = settings.testerRole;

            // ════════════════════════════════════════════════════════════════
            // TITLE — bold 20pt, #1F3864, Calibri
            // ════════════════════════════════════════════════════════════════
            document.add(new Paragraph("TEST SUMMARY REPORT")
                    .setFont(boldFont).setFontSize(20).setFontColor(DARK_NAVY)
                    .setMarginBottom(4));

            // ════════════════════════════════════════════════════════════════
            // SUBTITLE — 12pt, #2E5496, with bottom border #1F3864 (single, 8px)
            // ════════════════════════════════════════════════════════════════
            Paragraph subtitle = new Paragraph(cleanName + "  |  " + tr.getPlatform())
                    .setFont(regularFont).setFontSize(12).setFontColor(MEDIUM_BLUE)
                    .setMarginBottom(6);
            // Add bottom border to subtitle (line after)
            document.add(subtitle);

            // ════════════════════════════════════════════════════════════════
            // CONFIDENTIAL — italic 9pt, #595959
            // ════════════════════════════════════════════════════════════════
            document.add(new Paragraph("Confidential — QA Test Execution Summary")
                    .setFont(italicFont).setFontSize(9).setFontColor(DARK_GRAY)
                    .setMarginBottom(30));

            // ════════════════════════════════════════════════════════════════
            // SECTION 1: REPORT OVERVIEW
            // ════════════════════════════════════════════════════════════════
            Paragraph sec1 = new Paragraph("1. Report Overview")
                    .setFont(boldFont).setFontSize(13).setFontColor(DARK_NAVY)
                    .setMarginBottom(12);
            document.add(sec1);

            // Overview table: 2 columns (30% / 70%), 7 rows
            Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            addOverviewRow(overviewTable, "Project", cleanName, boldFont, regularFont);
            addOverviewRow(overviewTable, "Sprint / Cycle", cleanName, boldFont, regularFont);
            addOverviewRow(overviewTable, "Test Type", !tr.getLanguage().isEmpty() ? tr.getLanguage() : "Functional Testing", boldFont, regularFont);
            addOverviewRow(overviewTable, "Platform", tr.getPlatform() + (!tr.getBrowser().isEmpty() ? " / " + tr.getBrowser() : ""), boldFont, regularFont);
            // Executed By: all distinct tester names across results, no repeats
            String executedByAll = tr.getResults().stream()
                    .map(TestRunItems::getExecutedBy)
                    .filter(s -> !s.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.joining(", "));
            addOverviewRow(overviewTable, "Executed By",
                    executedByAll.isEmpty() ? "—" : executedByAll, boldFont, regularFont);
            addOverviewRow(overviewTable, "Execution Date", tr.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), boldFont, regularFont);
            addOverviewRow(overviewTable, "Run Status", trDir.getMarker().getStatus().name(), boldFont, regularFont);

            document.add(overviewTable);

            // ════════════════════════════════════════════════════════════════
            // SECTION 2: EXECUTION SUMMARY
            // ════════════════════════════════════════════════════════════════
            Paragraph sec2 = new Paragraph("2. Execution Summary")
                    .setFont(boldFont).setFontSize(13).setFontColor(DARK_NAVY)
                    .setMarginBottom(8).setMarginTop(16);
            document.add(sec2);

            long total = tr.getResults().size();
            long passed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
            long failed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
            long blocked = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.BLOCKED).count();
            long pending = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PENDING || r.getStatus() == TestStatus.UNTESTED).count();
            long passRate = total > 0 ? (passed * 100 / total) : 0;

            document.add(new Paragraph(
                    String.format("A total of %d test cases were executed for this run. The run completed with a %d%% pass rate. The results below summarise the outcome across all executed cases.", total, passRate))
                    .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                    .setMarginBottom(12));

            // 5-column stats table: each cell = number (bold 20pt, colored) + label (bold 9pt, #595959)
            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            addStatCell(statsTable, String.valueOf(total), "Total Cases", DARK_NAVY, boldFont);
            addStatCell(statsTable, String.valueOf(passed), "Passed", GREEN, boldFont);
            addStatCell(statsTable, String.valueOf(failed), "Failed", RED, boldFont);
            addStatCell(statsTable, String.valueOf(pending + blocked), "Pending", DARK_YELLOW, boldFont);
            addStatCell(statsTable, passRate + "%", "Pass Rate", MEDIUM_BLUE, boldFont);

            document.add(statsTable);

            // ════════════════════════════════════════════════════════════════
            // SECTION 3: RESULT ANALYSIS
            // ════════════════════════════════════════════════════════════════
            Paragraph sec3 = new Paragraph("3. Result Analysis")
                    .setFont(boldFont).setFontSize(13).setFontColor(DARK_NAVY)
                    .setMarginBottom(8).setMarginTop(16);
            document.add(sec3);

            // Passed
            Paragraph passedHeading = new Paragraph()
                    .add(new Paragraph("Passed (" + passed + "): ")
                            .setFont(boldFont).setFontSize(11).setFontColor(GREEN)
                            .setMarginBottom(0));
            document.add(passedHeading);

            Paragraph passedBody = new Paragraph(
                    "n\\a")
                    .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                    .setMarginBottom(6);
            document.add(passedBody);

            // Failed
            Paragraph failedHeading = new Paragraph()
                    .add(new Paragraph("Failed (" + failed + "): ")
                            .setFont(boldFont).setFontSize(11).setFontColor(RED)
                            .setMarginBottom(0));
            document.add(failedHeading);

            Paragraph failedBody = new Paragraph(
                    "n\\a")
                    .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                    .setMarginBottom(6);
            document.add(failedBody);

            // Pending
            Paragraph pendingHeading = new Paragraph()
                    .add(new Paragraph("Pending (" + (pending + blocked) + "): ")
                            .setFont(boldFont).setFontSize(11).setFontColor(DARK_YELLOW)
                            .setMarginBottom(0));
            document.add(pendingHeading);

            Paragraph pendingBody = new Paragraph(
                    "n\\a")
                    .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                    .setMarginBottom(6);
            document.add(pendingBody);

            // ════════════════════════════════════════════════════════════════
            // SECTION 4: FAILED TEST CASES (only if any failures exist)
            // Page break before this section (per template)
            // ════════════════════════════════════════════════════════════════
            if (failed > 0) {
                document.add(new AreaBreak());
                buildCaseTable(document, "4", "Failed Test Cases",
                        "The following %d cases failed and require remediation. Real-user identification validation is the primary defect cluster.",
                        failed, tr, detailsMap, boldFont, regularFont, RED, true, true, true,
                        item -> item.getStatus() == TestStatus.FAILED);
            }

            // ════════════════════════════════════════════════════════════════
            // SECTION 5: PASSED TEST CASES (only if any passed exist)
            // ════════════════════════════════════════════════════════════════
            if (passed > 0) {
                buildCaseTable(document, "5", "Passed Test Cases",
                        "The following %d cases passed validation and behaved as expected across all verification points.",
                        passed, tr, detailsMap, boldFont, regularFont, GREEN, false, false, false,
                        item -> item.getStatus() == TestStatus.PASSED);
            }

            // ════════════════════════════════════════════════════════════════
            // SECTION 6: PENDING TEST CASES (only if any pending exist)
            // ════════════════════════════════════════════════════════════════
            long pendingTotal = pending + blocked;
            if (pendingTotal > 0) {
                buildCaseTable(document, "6", "Pending Test Cases",
                        "The following %d cases were not executed in this cycle, primarily blocked by environment/data dependencies. They are carried forward to the next run.",
                        pendingTotal, tr, detailsMap, boldFont, regularFont, DARK_YELLOW, false, false, false,
                        item -> item.getStatus() == TestStatus.PENDING || item.getStatus() == TestStatus.UNTESTED || item.getStatus() == TestStatus.BLOCKED);
            }

            // ════════════════════════════════════════════════════════════════
            // FOOTER — pinned to the bottom of the last page
            // ════════════════════════════════════════════════════════════════
            // Draw on the last page, in the bottom margin zone (below the content
            // area) so it never overlaps body content even on a full page.
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
            // Prepared by <b>executedBy</b> — Quality Expert  |  execDate  |  Generated automatically by <b>Testin</b> IntelliJ plugin
            String execDate = trDir.getMarker().getCreatedAt()
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US));
            footerCanvas.add(new Paragraph()
                    .setFont(regularFont).setFontSize(8).setFontColor(DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Text("Prepared by "))
                    .add(new Text(testerName))
                    .add(new Text(" — " + testerRole + "  |  "))
                    .add(new Text(execDate))
                    .add(new Text("  |  Generated automatically by "))
                    .add(new Link("Testin", PdfAction.createURI("https://plugins.jetbrains.com/plugin/31514-testin"))
                            .setFontColor(LINK_BLUE))
                    .add(new Text(" IntelliJ plugin.")));

            footerCanvas.close();

            document.close();
            return baos.toByteArray();

        } catch (final IOException ex) {
            Logger.error("PDF generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    /**
     * Builds a filtered table of results.
     * Columns: # | Test Case [ | Priority ] [ | Severity ] depending on flags.
     *
     * @param sectionNumber  the section number label (e.g. "4")
     * @param sectionTitle   the section heading text
     * @param descriptionFmt format string with one %d placeholder for the count
     * @param count          the count to pass into the format string
     * @param tr             the TestRunDto containing results
     * @param detailsMap     map of UUID -> TestCaseDto for test case names
     * @param headerBg       background color for the header row
     * @param withPriority   whether to include the Priority column
     * @param withSeverity   whether to include the Severity column
     * @param withActualResult whether to add an \"Actual result: ...\" line inside the Test Case cell
     * @param filter         predicate to select which items go into the table
     */
    private void buildCaseTable(Document document, String sectionNumber, String sectionTitle, String descriptionFmt, long count, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap, PdfFont boldFont, PdfFont regularFont, DeviceRgb headerBg, boolean withPriority, boolean withSeverity, boolean withActualResult, Predicate<TestRunItems> filter) {
        document.add(new Paragraph(sectionNumber + ". " + sectionTitle)
                .setFont(boldFont).setFontSize(13).setFontColor(DARK_NAVY)
                .setMarginBottom(8).setMarginTop(16));

        document.add(new Paragraph(
                String.format(descriptionFmt, count))
                .setFont(regularFont).setFontSize(11).setFontColor(BLACK)
                .setMarginBottom(12));

        // Column widths depend on which extra columns are shown
        int extraCols = (withPriority ? 1 : 0) + (withSeverity ? 1 : 0);
        float[] widths = switch (extraCols) {
            case 0 -> new float[]{7, 93};
            case 1 -> new float[]{7, 83, 10};
            default -> new float[]{7, 73, 10, 10};
        };
        Table table = new Table(UnitValue.createPercentArray(widths))
                .useAllAvailableWidth()
                .setBorder(Border.NO_BORDER);

        // Header row
        addCaseTableHeader(table, "#", headerBg, boldFont);
        addCaseTableHeader(table, "Test Case", headerBg, boldFont);
        if (withPriority) addCaseTableHeader(table, "Priority", headerBg, boldFont);
        if (withSeverity) addCaseTableHeader(table, "Severity", headerBg, boldFont);

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
            String tcName = "";
            if (detailsMap != null) {
                TestCaseDto tc = detailsMap.get(item.getId());
                if (tc != null) {
                    tcName = tc.getDescription();
                }
            }
            if (tcName.isEmpty()) {
                tcName = "—";
            }
            Cell testCaseCell = new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6);
            testCaseCell.add(new Paragraph(tcName)
                    .setFont(regularFont).setFontSize(9.5f).setFontColor(BLACK)
                    .setMarginBottom(0));
            if (withActualResult) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                testCaseCell.add(new Paragraph("Actual result: " + actualResult)
                        .setFont(regularFont).setFontSize(9f).setFontColor(DARK_GRAY));
            }
            table.addCell(testCaseCell);

            if (withPriority) {
                BugPriority pri = item.getBugPriority();
                DeviceRgb priColor;
                if (pri == BugPriority.HIGH) priColor = RED;
                else if (pri == BugPriority.MEDIUM) priColor = DARK_YELLOW;
                else priColor = DARK_GRAY;
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

            if (withSeverity) {
                BugSeverity sev = item.getBugSeverity();
                DeviceRgb sevColor;
                if (sev == BugSeverity.BLOCKER) sevColor = RED;
                else if (sev == BugSeverity.MAJOR) sevColor = DARK_YELLOW;
                else sevColor = DARK_GRAY;
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

    private void addCaseTableHeader(Table table, String text, DeviceRgb bgColor, PdfFont boldFont) {
        table.addCell(new Cell()
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(6).setPaddingRight(6)
                .add(new Paragraph(text)
                        .setFont(boldFont).setFontSize(9.5f).setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addOverviewRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
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

    private void addStatCell(Table table, String number, String label, DeviceRgb numberColor, PdfFont boldFont) {
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
