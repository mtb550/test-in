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
import org.testin.model.markers.DetailRow;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.logger.Logger;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Display;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Predicate;

public final class TestRunPdfGenerator {


    private final @NotNull DeviceRgb DARK_NAVY = new DeviceRgb(0x1F, 0x38, 0x64);
    private final @NotNull DeviceRgb MEDIUM_BLUE = new DeviceRgb(0x2E, 0x54, 0x96);
    private final @NotNull DeviceRgb DARK_GRAY = new DeviceRgb(0x59, 0x59, 0x59);
    private final @NotNull DeviceRgb GREEN = new DeviceRgb(0x2E, 0x7D, 0x32);
    private final @NotNull DeviceRgb RED = new DeviceRgb(0xC0, 0x39, 0x2B);
    private final @NotNull DeviceRgb DARK_YELLOW = new DeviceRgb(0xB8, 0x86, 0x0B);
    private final @NotNull DeviceRgb LIGHT_BG = new DeviceRgb(0xF2, 0xF5, 0xFA);
    private final @NotNull DeviceRgb BORDER_GRAY = new DeviceRgb(0xD0, 0xD7, 0xE5);
    private final @NotNull DeviceRgb WHITE = new DeviceRgb(0xFF, 0xFF, 0xFF);
    private final @NotNull DeviceRgb BLACK = new DeviceRgb(0x00, 0x00, 0x00);
    private final @NotNull DeviceRgb LINK_BLUE = new DeviceRgb(0x00, 0x52, 0xCC);

    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        // try-with-resources: closing the Document also closes the PdfDocument and
        // PdfWriter, including on any failure path inside the body.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(new PdfDocument(new PdfWriter(baos)))) {
            PdfDocument pdf = document.getPdfDocument();

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

            // TITLE
            document.add(new Paragraph("TEST SUMMARY REPORT")
                    .setFont(boldFont).setFontSize(ReportFont.TITLE.pt()).setFontColor(DARK_NAVY)
                    .setMarginBottom(2));

            // SUBTITLE - the project, and the run under it. Two lines rather
            // than one, because they answer different questions: which project
            // this is, and which run of it.
            document.add(new Paragraph(ReportText.joined("  |  ", projectName, ReportText.joined(", ", tr.getPlatform(), tr.getComponent())))
                    .setFont(regularFont).setFontSize(ReportFont.SUBTITLE.pt()).setFontColor(MEDIUM_BLUE)
                    .setMarginBottom(0));

            // The rule closes the two names, above the notice - the notice is a
            // caption on the block, not part of it.
            document.add(new Paragraph(trDir.getName())
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(MEDIUM_BLUE)
                    .setPaddingBottom(4)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 2f))
                    .setMarginBottom(1));

            document.add(new Paragraph("Confidential — QA Test Execution Summary")
                    .setFont(italicFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                    .setMarginBottom(20));

            // SECTION 1: REPORT OVERVIEW
            Paragraph sec1 = new Paragraph("1. Report Overview")
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(12);
            document.add(sec1);


            // One traversal of the results serves the whole report: the counts
            // below, the pass rate, and who executed it.
            final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());

            Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            for (final DetailRow row : ReportOverview.rowsFor(projectName, trDir, tr, summary)) {
                addOverviewRow(overviewTable, row.caption(), row.value(), boldFont, regularFont);
            }

            document.add(overviewTable);

            // SECTION 2: EXECUTION SUMMARY
            Paragraph sec2 = new Paragraph("2. Execution Summary")
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(9)
                    .setMarginTop(20);
            document.add(sec2);

            document.add(new Paragraph(
                    String.format("This run holds %d test cases, of which %d were executed. Of those, %d%% passed. The results below summarize the outcome.",
                            summary.total(), summary.executed(), summary.passRate()))
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                    .setMarginBottom(12));


            // Six tiles, or seven when the run has removed cases: the total
            // counts them, so without a tile of their own the figures below the
            // total do not add up to it.
            final float[] tileWidths = new float[summary.hasRemoved() ? 7 : 6];
            Arrays.fill(tileWidths, 100f / tileWidths.length);

            Table statsTable = new Table(UnitValue.createPercentArray(tileWidths))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            addStatCell(statsTable, String.valueOf(summary.total()), "Total Cases", DARK_NAVY, boldFont);
            addStatCell(statsTable, String.valueOf(summary.passed()), TestStatus.PASSED.getLabel(), GREEN, boldFont);
            addStatCell(statsTable, String.valueOf(summary.failed()), TestStatus.FAILED.getLabel(), RED, boldFont);
            addStatCell(statsTable, String.valueOf(summary.blocked()), TestStatus.BLOCKED.getLabel(), DARK_YELLOW, boldFont);
            addStatCell(statsTable, String.valueOf(summary.untested()), TestStatus.UNTESTED.getLabel(), DARK_GRAY, boldFont);
            if (summary.hasRemoved()) {
                addStatCell(statsTable, String.valueOf(summary.removed()), TestStatus.REMOVED.getLabel(), DARK_GRAY, boldFont);
            }
            addStatCell(statsTable, summary.passRate() + "%", "Pass Rate", MEDIUM_BLUE, boldFont);

            document.add(statsTable);

            // SECTION 3: RESULT ANALYSIS
            // SECTION 3: RESULT ANALYSIS - only what the tester wrote.
            // A verdict they said nothing about prints no heading, and a run
            // nobody analysed prints no section, so the numbering below starts
            // at 3 instead of 4.
            final boolean analysed = ResultAnalysis.anyWrittenIn(tr.getResultAnalysis());

            if (analysed) {
                document.add(new Paragraph("3. Result Analysis")
                        .setFont(boldFont)
                        .setFontSize(ReportFont.SECTION.pt())
                        .setFontColor(DARK_NAVY)
                        .setPaddingBottom(3)
                        .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                        .setMarginBottom(9)
                        .setMarginTop(20));

                for (final ResultAnalysis section : ResultAnalysis.values()) {
                    final @NotNull String written = section.writtenIn(tr.getResultAnalysis());
                    if (written.isEmpty()) continue;

                    document.add(new Paragraph(section.heading(summary))
                            .setFont(boldFont).setFontSize(ReportFont.LEAD.pt())
                            .setFontColor(rgb(section.getHexColor()))
                            .setMarginBottom(2));

                    document.add(new Paragraph(written)
                            .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                            .setMarginBottom(8));
                }
            }

            // SECTIONS 4+: one case table per status, empty ones omitted. Numbered
            // as printed rather than per section, so a run with nothing blocked
            // does not jump from 5 to 7.
            int sectionNumber = analysed ? 4 : 3;
            for (final ReportSection section : ReportSection.values()) {
                final long count = section.count(summary);
                if (count == 0) continue;

                buildCaseTable(document, String.valueOf(sectionNumber++), section.getTitle(),
                        section.description(String.valueOf(count)), tr, detailsMap, boldFont, regularFont,
                        rgb(section.getHexColor()), rgb(section.textHex()), section.isWithFailureDetail(), section::matches);
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
                    .setFont(regularFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Text(Display.formatDate(ZonedDateTime.now())))
                    .add(new Text("  |  Generated automatically by "))
                    .add(new Link("Testin", PdfAction.createURI(ReportText.PLUGIN_URL))
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
     * A hex color as iText wants it.
     * <p>
     * The result-analysis sections declare their color once, as the hex string
     * all three report formats already used, so the paragraph in the PDF is the
     * same green as the one in the Word file and the HTML page.
     */
    private @NotNull DeviceRgb rgb(final @NotNull String hex) {
        return new DeviceRgb(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }

    private void buildCaseTable(final @NotNull Document document, final @NotNull String sectionNumber, final @NotNull String sectionTitle, final @NotNull String description, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont, final @NotNull DeviceRgb headerBg, final @NotNull DeviceRgb headerFg, final boolean withFailureDetail, final @NotNull Predicate<TestRunItems> filter) {
        document.add(new Paragraph(sectionNumber + ". " + sectionTitle)
                .setFont(boldFont)
                .setFontSize(ReportFont.SECTION.pt())
                .setFontColor(DARK_NAVY)
                .setPaddingBottom(3)
                .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                .setMarginBottom(9)
                .setMarginTop(20));

        document.add(new Paragraph(description)
                .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                .setMarginBottom(12));

        // Sized by what is in them rather than by a share of the page each.
        // Fixed shares meant guessing how wide "Enhancement" is: too small and it
        // wrapped, too large and the description - the column anyone actually
        // reads - lost the room for nothing. Under auto layout the narrow columns
        // take what their longest word needs and the description takes the rest.
        Table table = new Table(withFailureDetail ? 4 : 2)
                .useAllAvailableWidth()
                .setAutoLayout()
                .setBorder(Border.NO_BORDER);

        // Header row
        addCaseTableHeader(table, "#", headerBg, headerFg, boldFont);
        addCaseTableHeader(table, "Test Case", headerBg, headerFg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, "Priority", headerBg, headerFg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, "Severity", headerBg, headerFg, boldFont);

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
                            .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)));

            // Test Case column
            final @NotNull String caseName = ReportedCase.of(detailsMap, item.getId()).getDescription();
            final @NotNull String tcName = caseName.isEmpty() ? "—" : caseName;
            final @NotNull Cell testCaseCell = new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6);
            testCaseCell.add(new Paragraph(tcName)
                    .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                    .setMarginBottom(0));
            if (withFailureDetail) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                testCaseCell.add(new Paragraph("Actual result: " + actualResult)
                        .setFont(regularFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY));

                // Only when there is one. The actual result prints an em dash
                // for absent because a failure with nothing written about it is
                // worth noticing; an absent stacktrace is not worth a line.
                if (!item.getStacktrace().isBlank()) {
                    testCaseCell.add(new Paragraph(item.getStacktrace())
                            .setFont(regularFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY));
                }
            }
            table.addCell(testCaseCell);

            if (withFailureDetail) {
                BugPriority pri = item.getBugPriority();
                DeviceRgb priColor = rgb(pri.getEmphasis().getHexColor());
                String priText = pri.getName();
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(new Paragraph(priText)
                                .setFont(boldFont).setFontSize(ReportFont.BODY.pt()).setFontColor(priColor)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            if (withFailureDetail) {
                BugSeverity sev = item.getBugSeverity();
                DeviceRgb sevColor = rgb(sev.getEmphasis().getHexColor());
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "—";
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(new Paragraph(sevText)
                                .setFont(boldFont).setFontSize(ReportFont.BODY.pt()).setFontColor(sevColor)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            idx++;
        }

        document.add(table);
    }

    private void addCaseTableHeader(final @NotNull Table table, final @NotNull String text, final @NotNull DeviceRgb bgColor, final @NotNull DeviceRgb textColor, final @NotNull PdfFont boldFont) {
        table.addCell(new Cell()
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(6).setPaddingRight(6)
                .add(new Paragraph(text)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(textColor)
                        .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addOverviewRow(final @NotNull Table table, final @NotNull String label, final @NotNull String value, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        table.addCell(new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(new Paragraph(label)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(DARK_NAVY)));

        table.addCell(new Cell()
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(new Paragraph(value)
                        .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)));
    }

    private void addStatCell(final @NotNull Table table, final @NotNull String number, final @NotNull String label, final @NotNull DeviceRgb numberColor, final @NotNull PdfFont boldFont) {
        Cell cell = new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6);

        cell.add(new Paragraph(number)
                .setFont(boldFont).setFontSize(ReportFont.FIGURE.pt()).setFontColor(numberColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        cell.add(new Paragraph(label)
                .setFont(boldFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        table.addCell(cell);
    }
}
