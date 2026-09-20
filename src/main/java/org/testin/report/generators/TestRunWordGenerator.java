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
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.TestRunSummary;
import org.testin.report.ReportTile;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.testin.logger.Logger;
import org.testin.model.BugIssueUrl;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class TestRunWordGenerator {
    final String NO_BORDER = "";

    final String DARK_NAVY = "1F3864";
    final String MEDIUM_BLUE = "2E5496";
    final String DARK_GRAY = "595959";
    final String LINK_BLUE = ReportText.LINK_BLUE;
    final String GREEN = "2E7D32";
    final String RED = "C0392B";
    final String DARK_YELLOW = "B8860B";
    final String LIGHT_BG = "F2F5FA";
    final String BORDER_GRAY = "D0D7E5";
    final String WHITE = "FFFFFF";
    final String BLACK = "000000";

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {
                final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

                addText(doc, Bundle.message("report.title"), ReportFont.TITLE.ptRounded(), true, DARK_NAVY, NO_BORDER, 2);

                addText(doc, ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(trDir.getMarker()), TestRunConfiguration.COMPONENT.valueIn(trDir.getMarker()))),
                        ReportFont.SUBTITLE.ptRounded(), false, MEDIUM_BLUE, NO_BORDER, 0);
                addText(doc, trDir.getName(), ReportFont.LEAD.ptRounded(), false, MEDIUM_BLUE, DARK_NAVY, 1);

                XWPFParagraph conf = addText(doc, Bundle.message("report.confidential"), ReportFont.CAPTION.ptRounded(), false, DARK_GRAY, NO_BORDER, 20);
                setItalic(conf);

                addHeading(doc, Bundle.message("report.heading.overview"), 0, 15);

                final @NotNull TestRunSummary summary = TestRunSummary.of(tr.getResults());

                XWPFTable overviewTable = doc.createTable(1, 2);
                overviewTable.setWidth("100%");
                overviewTable.setWidthType(TableWidthType.PCT);
                setTableWidths(overviewTable, 30, 70);

                int overviewRow = 0;
                for (final DetailRow row : ReportOverview.rowsFor(projectName, trDir, tr, summary)) {
                    addOverviewRow(overviewTable, overviewRow++, row.caption(), row.value());
                }

                setTableBorders(overviewTable);

                addHeading(doc, Bundle.message("report.heading.execution"), 20, 12);

                addText(doc, Bundle.message("report.summary.named", trDir.getName(),
                        String.valueOf(summary.total()), String.valueOf(summary.executed()), summary.passRate() + "%"),
                        ReportFont.LEAD.ptRounded(), false, BLACK, NO_BORDER, 12);

                final @NotNull List<ReportTile> headline = ReportTile.shownFor(summary);
                final int tiles = headline.size();

                XWPFTable statsTable = doc.createTable(1, tiles);
                statsTable.setWidth("100%");
                statsTable.setWidthType(TableWidthType.PCT);
                setTableBorders(statsTable);
                setTableWidths(statsTable, evenWidths(tiles));

                for (int tile = 0; tile < tiles; tile++) {
                    final @NotNull ReportTile figure = headline.get(tile);
                    addStatCell(statsTable, tile, figure.valueIn(summary), figure.getLabel(), figure.getHex());
                }

                final boolean analyzed = ResultAnalysis.anyWrittenIn(trDir.getMarker().getResultAnalysis());

                if (analyzed) {
                    addHeading(doc, Bundle.message("report.heading.analysis"), 20, 12);

                    for (final ResultAnalysis section : ResultAnalysis.values()) {
                        final @NotNull String written = section.writtenIn(trDir.getMarker().getResultAnalysis());
                        if (written.isEmpty()) continue;

                        addColoredCount(doc, section.heading(summary), section.getHexColor());
                        addText(doc, written, ReportFont.BODY.ptRounded(), false, BLACK, NO_BORDER, 8);
                    }
                }

                int sectionNumber = analyzed ? 4 : 3;
                for (final ReportSection section : ReportSection.values()) {
                    final long count = section.count(summary);
                    if (count == 0) continue;

                    buildCaseTable(doc, String.valueOf(sectionNumber++), section.getTitle(),
                            section.description(String.valueOf(count)), tr, detailsMap, section.getHexColor(), section.textHex(),
                            section.isWithFailureDetail(), section::matches);
                }

                addFooter(doc, Display.formatDate(ZonedDateTime.now()));

                applyPageMargins(doc);

                doc.write(baos);
            }
            return baos.toByteArray();

        } catch (final IOException ex) {
            Logger.error("Word generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private @NotNull XWPFParagraph addText(final @NotNull XWPFDocument doc, final @NotNull String text, final int size, final boolean bold, final @NotNull String color, final @NotNull String bottomBorder, final int spacingAfterPt) {
        final @NotNull XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfterPt * 20);
        final @NotNull XWPFRun run = p.createRun();
        writeLines(run, text, false);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
        run.setBold(bold);
        run.setColor(color);
        if (!bottomBorder.isEmpty()) {
            final @NotNull CTBorder bottom = p.getCTPPr().addNewPBdr().addNewBottom();
            bottom.setVal(STBorder.Enum.forString("single"));
            bottom.setSz(BigInteger.valueOf(16));
            bottom.setColor(bottomBorder);
        }
        return p;
    }

    private void setItalic(final @NotNull XWPFParagraph p) {
        for (final XWPFRun run : p.getRuns()) {
            run.setItalic(true);
        }
    }

    private void addHeading(final @NotNull XWPFDocument doc, final @NotNull String text, final int beforePt, final int afterPt) {
        final @NotNull XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(beforePt * 20);
        p.setSpacingAfter(afterPt * 20);
        final @NotNull XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(ReportFont.SECTION.ptRounded());
        run.setFontFamily("Calibri");
        run.setBold(true);
        run.setColor(DARK_NAVY);
        final @NotNull CTBorder headingBottom = p.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(BigInteger.valueOf(8));
        headingBottom.setColor(DARK_NAVY);
    }

    private void addOverviewRow(final @NotNull XWPFTable table, final int rowIdx, final @NotNull String label, final @NotNull String value) {
        final @NotNull XWPFTableRow row = rowIdx == 0 ? table.getRow(0) : table.createRow();
        final @NotNull XWPFTableCell labelCell = row.getCell(0);
        final @NotNull XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, LIGHT_BG);
        setCellPadding(labelCell, 4, 8, 4, 8);
        setCellPadding(valueCell, 4, 8, 4, 8);
        setCellText(labelCell, label, ReportFont.HEADING.ptRounded(), true, DARK_NAVY);
        setCellText(valueCell, value, ReportFont.BODY.ptRounded(), false, BLACK);
    }

    private void addStatCell(final @NotNull XWPFTable table, final int col, final @NotNull String number, final @NotNull String label, final @NotNull String numberColor) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        final @NotNull XWPFTableCell cell = row.getCell(col);
        shadeCell(cell, LIGHT_BG);
        setCellPadding(cell, 8, 6, 8, 6);
        setCellText(cell, number, ReportFont.FIGURE.ptRounded(), true, numberColor);

        cell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

        final @NotNull XWPFParagraph lp = cell.addParagraph();
        lp.setAlignment(ParagraphAlignment.CENTER);
        lp.setSpacingBefore(80);
        final @NotNull XWPFRun lrun = lp.createRun();
        lrun.setText(label);
        lrun.setFontSize(ReportFont.SMALL.ptRounded());
        lrun.setFontFamily("Calibri");
        lrun.setBold(true);
        lrun.setColor(DARK_GRAY);
    }

    private void addColoredCount(final @NotNull XWPFDocument doc, final @NotNull String heading, final @NotNull String headingColor) {
        final @NotNull XWPFParagraph hp = doc.createParagraph();
        hp.setSpacingAfter(120);

        final @NotNull XWPFRun hrun = hp.createRun();
        hrun.setText(heading);
        hrun.setFontSize(ReportFont.LEAD.ptRounded());
        hrun.setFontFamily("Calibri");
        hrun.setBold(true);
        hrun.setColor(headingColor);
    }

    private void buildCaseTable(final @NotNull XWPFDocument doc, final @NotNull String sectionNumber, final @NotNull String sectionTitle, final @NotNull String description, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull String headerBg, final @NotNull String headerFg, final boolean withFailureDetail, final @NotNull Predicate<TestRunItems> filter) {
        addHeading(doc, sectionNumber + ". " + sectionTitle, 20, 12);
        addText(doc, description, ReportFont.LEAD.ptRounded(), false, BLACK, NO_BORDER, 12);

        int cols = withFailureDetail ? 4 : 2;
        XWPFTable table = doc.createTable(1, cols);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        XWPFTableRow headerRow = table.getRow(0);
        addCaseHeader(headerRow, 0, "#", headerBg, headerFg);
        addCaseHeader(headerRow, 1, Bundle.message("caption.test.case"), headerBg, headerFg);
        if (withFailureDetail) addCaseHeader(headerRow, 2, RunEditorAttributes.BUG_PRIORITY.getName(), headerBg, headerFg);
        if (withFailureDetail) addCaseHeader(headerRow, 3, RunEditorAttributes.BUG_SEVERITY.getName(), headerBg, headerFg);

        int idx = 1;
        boolean alt = true;
        for (TestRunItems item : tr.getResults()) {
            if (!filter.test(item)) continue;

            String rowBg = alt ? LIGHT_BG : WHITE;
            alt = !alt;

            XWPFTableRow row = table.createRow();
            XWPFTableCell numCell = row.getCell(0);
            shadeCell(numCell, rowBg);
            setCellPadding(numCell, 4, 6, 4, 6);
            setCellText(numCell, String.valueOf(idx), ReportFont.BODY.ptRounded(), false, DARK_GRAY);
            numCell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

            XWPFTableCell tcCell = row.getCell(1);
            shadeCell(tcCell, rowBg);
            setCellPadding(tcCell, 4, 6, 4, 6);
            final @NotNull String caseName = ReportedCase.of(detailsMap, item.getId()).getDescription();
            final @NotNull String tcName = caseName.isEmpty() ? "—" : caseName;
            setCellText(tcCell, tcName, ReportFont.BODY.ptRounded(), false, BLACK);

            if (withFailureDetail) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                final @NotNull XWPFParagraph ap = tcCell.addParagraph();
                styledRun(ap.createRun(), Bundle.message("report.actual.result", actualResult), ReportFont.SMALL, DARK_GRAY);

                item.bugIssue().ifPresent(url -> {
                    styledRun(ap.createRun(), " (", ReportFont.SMALL, DARK_GRAY);
                    final @NotNull XWPFHyperlinkRun issue = ap.createHyperlinkRun(url);
                    styledRun(issue, BugIssueUrl.shortReference(url), ReportFont.SMALL, LINK_BLUE);
                    issue.setUnderline(UnderlinePatterns.SINGLE);
                    styledRun(ap.createRun(), ")", ReportFont.SMALL, DARK_GRAY);
                });
            }

            if (withFailureDetail) {
                XWPFTableCell priCell = row.getCell(2);
                shadeCell(priCell, rowBg);
                setCellPadding(priCell, 4, 6, 4, 6);
                BugPriority pri = item.getBugPriority();
                String priColor = pri.getEmphasis().getHexColor();
                setCellText(priCell, pri.getLabel(), ReportFont.BODY.ptRounded(), true, priColor);
            }

            if (withFailureDetail) {
                XWPFTableCell sevCell = row.getCell(3);
                shadeCell(sevCell, rowBg);
                setCellPadding(sevCell, 4, 6, 4, 6);
                BugSeverity sev = item.getBugSeverity();
                String sevColor = sev.getEmphasis().getHexColor();
                String sevText = sev.getLabel();
                if (sevText.isEmpty()) sevText = "—";
                setCellText(sevCell, sevText, ReportFont.BODY.ptRounded(), true, sevColor);
            }

            idx++;
        }

        setTableBorders(table);
        autoFitToContent(table);
    }

    private void autoFitToContent(final @NotNull XWPFTable table) {
        final @NotNull CTTblPr properties = table.getCTTbl().getTblPr();
        final @NotNull CTTblLayoutType layout =
                properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcPr cellProperties = getTcPr(cell);
                (cellProperties.isSetTcW() ? cellProperties.getTcW() : cellProperties.addNewTcW())
                        .setType(STTblWidth.AUTO);
            }
        }
    }

    private void addCaseHeader(final @NotNull XWPFTableRow headerRow, final int col, final @NotNull String text, final @NotNull String bgColor, final @NotNull String textColor) {
        final @NotNull XWPFTableCell cell = headerRow.getCell(col);
        shadeCell(cell, bgColor);
        setCellPadding(cell, 5, 6, 5, 6);
        setCellText(cell, text, ReportFont.HEADING.ptRounded(), true, textColor);
    }

    private void writeLines(final @NotNull XWPFRun run, final @NotNull String text, final boolean replaceFirst) {
        final @NotNull List<String> lines = text.lines().toList();

        if (lines.isEmpty()) {
            run.setText("", 0);
            return;
        }

        if (replaceFirst) run.setText(lines.getFirst(), 0);
        else run.setText(lines.getFirst());

        for (final String line : lines.subList(1, lines.size())) {
            run.addBreak();
            run.setText(line);
        }
    }

    private void setCellText(final @NotNull XWPFTableCell cell, final @NotNull String text, final int size, final boolean bold, final @NotNull String color) {
        final @NotNull XWPFParagraph p = cell.getParagraphs().getFirst();
        if (p.getRuns().isEmpty()) {
            final @NotNull XWPFRun run = p.createRun();
            writeLines(run, text, false);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);

        } else {
            final @NotNull XWPFRun run = p.getRuns().getFirst();
            writeLines(run, text, true);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);
        }
    }

    private void shadeCell(final @NotNull XWPFTableCell cell, final @NotNull String hex) {
        getTcPr(cell).addNewShd().setFill(hex);
    }

    private void setCellPadding(final @NotNull XWPFTableCell cell, final int topPt, final int leftPt, final int bottomPt, final int rightPt) {
        final @NotNull CTTcMar mar = getTcPr(cell).addNewTcMar();
        final @NotNull CTTblWidth top = mar.addNewTop();
        top.setW(topPt * 20);
        top.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth left = mar.addNewLeft();
        left.setW(leftPt * 20);
        left.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth bottom = mar.addNewBottom();
        bottom.setW(bottomPt * 20);
        bottom.setType(STTblWidth.Enum.forString("dxa"));
        final @NotNull CTTblWidth right = mar.addNewRight();
        right.setW(rightPt * 20);
        right.setType(STTblWidth.Enum.forString("dxa"));
    }

    private @NotNull CTTcPr getTcPr(final @NotNull XWPFTableCell cell) {
        final @NotNull CTTc ct = cell.getCTTc();
        return ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
    }

    private void setTableBorders(final @NotNull XWPFTable table) {
        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcBorders borders = getTcPr(cell).addNewTcBorders();
                final @NotNull CTBorder top = borders.addNewTop();
                top.setColor(BORDER_GRAY);
                top.setSz(BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder bottom = borders.addNewBottom();
                bottom.setColor(BORDER_GRAY);
                bottom.setSz(BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder left = borders.addNewLeft();
                left.setColor(BORDER_GRAY);
                left.setSz(BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                final @NotNull CTBorder right = borders.addNewRight();
                right.setColor(BORDER_GRAY);
                right.setSz(BigInteger.valueOf(4));
                right.setVal(STBorder.Enum.forString("single"));
            }
        }
    }

    private int @NotNull [] evenWidths(final int columns) {
        final int[] widths = new int[columns];
        Arrays.fill(widths, 100 / columns);

        for (int i = 0; i < 100 % columns; i++) widths[i]++;

        return widths;
    }

    private void setTableWidths(final @NotNull XWPFTable table, final int... percents) {
        final @NotNull XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            getTcPr(row.getCell(i)).addNewTcW().setW(percents[i] * 100);
            getTcPr(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(final @NotNull XWPFDocument doc, final @NotNull String date) {
        final @NotNull XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        final @NotNull XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        styledRun(p.createRun(), date + Bundle.message("report.footer.prefix"), ReportFont.CAPTION, DARK_GRAY);

        final @NotNull XWPFHyperlinkRun link = p.createHyperlinkRun(ReportText.PLUGIN_URL);
        styledRun(link, "Testin", ReportFont.CAPTION, LINK_BLUE);
        link.setUnderline(UnderlinePatterns.SINGLE);

        styledRun(p.createRun(), Bundle.message("report.footer.suffix"), ReportFont.CAPTION, DARK_GRAY);
    }

    private void styledRun(final @NotNull XWPFRun run, final @NotNull String text, final @NotNull ReportFont font, final @NotNull String color) {
        run.setText(text);
        run.setFontSize(font.ptRounded());
        run.setFontFamily("Calibri");
        run.setColor(color);
    }

    private void applyPageMargins(final @NotNull XWPFDocument doc) {
        final @NotNull CTBody body = doc.getDocument().getBody();
        final @NotNull CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        final @NotNull CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();

        final long sideTwips = 720L;
        final long endTwips = 1440L;

        pgMar.setLeft(sideTwips);
        pgMar.setRight(sideTwips);
        pgMar.setTop(endTwips);
        pgMar.setBottom(endTwips);
    }
}
