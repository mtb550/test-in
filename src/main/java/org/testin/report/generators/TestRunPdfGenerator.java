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

import org.testin.model.TestRunConfiguration;
import org.testin.testrun.RunEditorAttributes;
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
import org.testin.report.ReportTile;
import org.testin.logger.Logger;
import org.testin.model.BugIssueUrl;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import com.intellij.openapi.util.text.StringUtil;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class TestRunPdfGenerator {
    private @NotNull Optional<PdfFont> printsWith = Optional.empty();

    private final @NotNull Set<String> leftOut = new LinkedHashSet<>();

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
    private final @NotNull DeviceRgb LINK_BLUE = rgb(ReportText.LINK_BLUE);

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document document = new Document(pdf, pdf.getDefaultPageSize(), false)) {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Rule-REPORT-018
            printsWith = Optional.of(regularFont);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

            document.add(para(Bundle.message("report.title"))
                    .setFont(boldFont).setFontSize(ReportFont.TITLE.pt()).setFontColor(DARK_NAVY)
                    .setMarginBottom(2));

            document.add(para(ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(trDir.getMarker()), TestRunConfiguration.COMPONENT.valueIn(trDir.getMarker()))))
                    .setFont(regularFont).setFontSize(ReportFont.SUBTITLE.pt()).setFontColor(MEDIUM_BLUE)
                    .setMarginBottom(0));

            document.add(para(trDir.getName())
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(MEDIUM_BLUE)
                    .setPaddingBottom(4)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 2f))
                    .setMarginBottom(1));

            document.add(para(Bundle.message("report.confidential"))
                    .setFont(italicFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                    .setMarginBottom(20));

            Paragraph sec1 = para(Bundle.message("report.heading.overview"))
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(12);
            document.add(sec1);

            final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());

            Table overviewTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            for (final DetailRow row : ReportOverview.rowsFor(projectName, trDir, tr, summary)) {
                addOverviewRow(overviewTable, row.caption(), row.value(), boldFont, regularFont);
            }

            document.add(overviewTable);

            Paragraph sec2 = para(Bundle.message("report.heading.execution"))
                    .setFont(boldFont)
                    .setFontSize(ReportFont.SECTION.pt())
                    .setFontColor(DARK_NAVY)
                    .setPaddingBottom(3)
                    .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                    .setMarginBottom(9)
                    .setMarginTop(20);
            document.add(sec2);

            document.add(para(
                    Bundle.message("report.summary.named", trDir.getName(),
                            String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"))
                    .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                    .setMarginBottom(12));

            final @NotNull java.util.List<ReportTile> tiles = ReportTile.shownFor(summary);

            final float[] tileWidths = new float[tiles.size()];
            Arrays.fill(tileWidths, 100f / tileWidths.length);

            Table statsTable = new Table(UnitValue.createPercentArray(tileWidths))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            for (final ReportTile tile : tiles) {
                addStatCell(statsTable, tile.valueIn(summary), tile.getLabel(), rgb(tile.getHex()), boldFont);
            }

            document.add(statsTable);

            final boolean analyzed = ResultAnalysis.anyWrittenIn(trDir.getMarker().getResultAnalysis());

            if (analyzed) {
                document.add(para(Bundle.message("report.heading.analysis"))
                        .setFont(boldFont)
                        .setFontSize(ReportFont.SECTION.pt())
                        .setFontColor(DARK_NAVY)
                        .setPaddingBottom(3)
                        .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                        .setMarginBottom(9)
                        .setMarginTop(20));

                for (final ResultAnalysis section : ResultAnalysis.values()) {
                    final @NotNull String written = section.writtenIn(trDir.getMarker().getResultAnalysis());
                    if (written.isEmpty()) continue;

                    document.add(para(section.heading(summary))
                            .setFont(boldFont).setFontSize(ReportFont.LEAD.pt())
                            .setFontColor(rgb(section.getHexColor()))
                            .setMarginBottom(2));

                    document.add(para(written)
                            .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                            .setMarginBottom(8));
                }
            }

            int sectionNumber = analyzed ? 4 : 3;
            for (final ReportSection section : ReportSection.values()) {
                final long count = section.count(summary);
                if (count == 0) continue;

                buildCaseTable(document, String.valueOf(sectionNumber++), section.getTitle(),
                        section.description(String.valueOf(count)), tr, boldFont, regularFont,
                        rgb(section.getHexColor()), rgb(section.textHex()), section.isWithFailureDetail(), section::matches);
            }

            float pageWidth = pdf.getDefaultPageSize().getWidth();
            float leftMargin = document.getLeftMargin();
            float rightMargin = document.getRightMargin();

            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                final @NotNull Canvas footerCanvas = new Canvas(pdf.getPage(page),
                        new Rectangle(leftMargin, 0, pageWidth - leftMargin - rightMargin, 28));

                final @NotNull PdfCanvas pdfCanvas = footerCanvas.getPdfCanvas();
                pdfCanvas.setStrokeColor(BORDER_GRAY);
                pdfCanvas.setLineWidth(1.0f);
                pdfCanvas.moveTo(leftMargin, 34);
                pdfCanvas.lineTo(pageWidth - rightMargin, 34);
                pdfCanvas.stroke();

                footerCanvas.add(para()
                        .setFont(regularFont).setFontSize(ReportFont.CAPTION.pt()).setFontColor(DARK_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .add(text(Display.formatDate(ZonedDateTime.now())))
                        .add(text(Bundle.message("report.footer.prefix")))
                        .add(new Link("Testin", PdfAction.createURI(ReportText.PLUGIN_URL))
                                .setFontColor(LINK_BLUE))
                        .add(text(Bundle.message("report.footer.suffix"))));

                footerCanvas.close();
            }

            document.close();
            sayWhatWasLeftOut(p);
            return baos.toByteArray();

        } catch (final Exception ex) {
            Logger.error("PDF generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private @NotNull DeviceRgb rgb(final @NotNull String hex) {
        return new DeviceRgb(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16));
    }

    private void buildCaseTable(final @NotNull Document document, final @NotNull String sectionNumber, final @NotNull String sectionTitle, final @NotNull String description, final @NotNull TestRunDto tr, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont, final @NotNull DeviceRgb headerBg, final @NotNull DeviceRgb headerFg, final boolean withFailureDetail, final @NotNull Predicate<TestRunItems> filter) {
        document.add(para(sectionNumber + ". " + sectionTitle)
                .setFont(boldFont)
                .setFontSize(ReportFont.SECTION.pt())
                .setFontColor(DARK_NAVY)
                .setPaddingBottom(3)
                .setBorderBottom(new SolidBorder(DARK_NAVY, 1f))
                .setMarginBottom(9)
                .setMarginTop(20));

        document.add(para(description)
                .setFont(regularFont).setFontSize(ReportFont.LEAD.pt()).setFontColor(BLACK)
                .setMarginBottom(12));

        Table table = new Table(withFailureDetail ? 4 : 2)
                .useAllAvailableWidth()
                .setAutoLayout()
                .setBorder(Border.NO_BORDER);

        addCaseTableHeader(table, "#", headerBg, headerFg, boldFont);
        addCaseTableHeader(table, Bundle.message("caption.test.case"), headerBg, headerFg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, RunEditorAttributes.BUG_PRIORITY.getName(), headerBg, headerFg, boldFont);
        if (withFailureDetail) addCaseTableHeader(table, RunEditorAttributes.BUG_SEVERITY.getName(), headerBg, headerFg, boldFont);

        int idx = 1;
        boolean alt = true;
        for (TestRunItems item : tr.getResults()) {
            if (!filter.test(item)) continue;

            DeviceRgb rowBg = alt ? LIGHT_BG : WHITE;
            alt = !alt;

            table.addCell(new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                    .add(para(String.valueOf(idx))
                            .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(DARK_GRAY)
                            .setTextAlignment(TextAlignment.CENTER)));

            final @NotNull String caseName = item.shownCase().getDescription();
            final @NotNull String tcName = caseName.isEmpty() ? "—" : caseName;
            final @NotNull Cell testCaseCell = new Cell()
                    .setBackgroundColor(rowBg)
                    .setBorder(new SolidBorder(BORDER_GRAY, 1))
                    .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6);
            testCaseCell.add(para(tcName)
                    .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)
                    .setMarginBottom(0));
            if (withFailureDetail) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                final @NotNull Paragraph actual = para(Bundle.message("report.actual.result", actualResult))
                        .setFont(regularFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY);

                item.bugIssue().ifPresent(url -> actual.add(text(" ("))
                        .add(new Link(BugIssueUrl.shortReference(url), PdfAction.createURI(url)).setFontColor(LINK_BLUE))
                        .add(text(")")));
                testCaseCell.add(actual);
            }
            table.addCell(testCaseCell);

            if (withFailureDetail) {
                BugPriority pri = item.getBugPriority();
                DeviceRgb priColor = rgb(pri.getEmphasis().getHexColor());
                String priText = pri.getLabel();
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(para(priText)
                                .setFont(boldFont).setFontSize(ReportFont.BODY.pt()).setFontColor(priColor)
                                .setTextAlignment(TextAlignment.CENTER)));
            }

            if (withFailureDetail) {
                BugSeverity sev = item.getBugSeverity();
                DeviceRgb sevColor = rgb(sev.getEmphasis().getHexColor());
                String sevText = sev.getLabel();
                if (sevText.isEmpty()) sevText = "—";
                table.addCell(new Cell()
                        .setBackgroundColor(rowBg)
                        .setBorder(new SolidBorder(BORDER_GRAY, 1))
                        .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(6).setPaddingRight(6)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .add(para(sevText)
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
                .add(para(text)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(textColor)
                        .setTextAlignment(TextAlignment.CENTER)));
    }

    private void addOverviewRow(final @NotNull Table table, final @NotNull String label, final @NotNull String value, final @NotNull PdfFont boldFont, final @NotNull PdfFont regularFont) {
        table.addCell(new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(para(label)
                        .setFont(boldFont).setFontSize(ReportFont.HEADING.pt()).setFontColor(DARK_NAVY)));

        table.addCell(new Cell()
                .setBorder(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(4).setPaddingBottom(4).setPaddingLeft(8).setPaddingRight(8)
                .add(para(value)
                        .setFont(regularFont).setFontSize(ReportFont.BODY.pt()).setFontColor(BLACK)));
    }

    private void addStatCell(final @NotNull Table table, final @NotNull String number, final @NotNull String label, final @NotNull DeviceRgb numberColor, final @NotNull PdfFont boldFont) {
        Cell cell = new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(8).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6);

        cell.add(para(number)
                .setFont(boldFont).setFontSize(ReportFont.FIGURE.pt()).setFontColor(numberColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        cell.add(para(label)
                .setFont(boldFont).setFontSize(ReportFont.SMALL.pt()).setFontColor(DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        table.addCell(cell);
    }

    private @NotNull Paragraph para(final @NotNull String text) {
        return new Paragraph(printable(text));
    }

    private @NotNull Paragraph para() {
        return new Paragraph();
    }

    private @NotNull Text text(final @NotNull String text) {
        return new Text(printable(text));
    }

    // UC-REPORT-001, Rule-REPORT-018
    private @NotNull String printable(final @NotNull String text) {
        printsWith.ifPresent(font -> {
            final boolean loses = text.codePoints()
                    .filter(cp -> !Character.isWhitespace(cp) && !Character.isISOControl(cp))
                    .anyMatch(cp -> !font.containsGlyph(cp));
            if (loses) leftOut.add(text);
        });
        return text;
    }

    // UC-REPORT-001, Rule-REPORT-018
    private void sayWhatWasLeftOut(final @NotNull Project p) {
        if (leftOut.isEmpty()) return;

        final @NotNull String howMuch = leftOut.size() == 1
                ? Bundle.message("report.pdf.left.out.one")
                : Bundle.message("report.pdf.left.out.many", String.valueOf(leftOut.size()));
        final @NotNull String example = StringUtil.shortenTextWithEllipsis(leftOut.iterator().next().strip(), 60, 0);

        Logger.warn("The PDF left characters out of " + leftOut.size() + " text(s) its font cannot print");
        Services.getInstance(p, Notifier.class).warn(p, Bundle.message("report.pdf.left.out.title"),
                Bundle.message("report.pdf.left.out.message", howMuch, example));
    }
}
