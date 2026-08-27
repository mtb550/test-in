package org.testin.report.generators;

import org.testin.model.TestRunConfiguration;
import org.testin.model.RunEditorAttributes;
import com.intellij.openapi.project.Project;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
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
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class TestRunWordGenerator {

    /**
     * No rule under the paragraph, said as a color of no color rather than as a
     * null the border writer would have to check (#71).
     */
    final String NO_BORDER = "";

    final String DARK_NAVY = "1F3864";
    final String MEDIUM_BLUE = "2E5496";
    final String DARK_GRAY = "595959";
    final String LINK_BLUE = "0052CC";
    final String GREEN = "2E7D32";
    final String RED = "C0392B";
    final String DARK_YELLOW = "B8860B";
    final String LIGHT_BG = "F2F5FA";
    final String BORDER_GRAY = "D0D7E5";
    final String WHITE = "FFFFFF";
    final String BLACK = "000000";

    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {

                final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

                addText(doc, "TEST SUMMARY REPORT", ReportFont.TITLE.ptRounded(), true, DARK_NAVY, NO_BORDER, 2);

                // The project, and the run under it - the same two lines the PDF
                // prints, for the same reason.
                addText(doc, ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(tr), TestRunConfiguration.COMPONENT.valueIn(tr))),
                        ReportFont.SUBTITLE.ptRounded(), false, MEDIUM_BLUE, NO_BORDER, 0);
                // The rule closes the two names, above the notice.
                addText(doc, trDir.getName(), ReportFont.LEAD.ptRounded(), false, MEDIUM_BLUE, DARK_NAVY, 1);

                XWPFParagraph conf = addText(doc, "Confidential — QA Test Execution Summary", ReportFont.CAPTION.ptRounded(), false, DARK_GRAY, NO_BORDER, 20);
                setItalic(conf);

                addHeading(doc, "1. Report Overview", 0, 15);

                // One traversal of the results serves the whole report: the
                // counts below, the pass rate, and who executed it.
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

                addHeading(doc, "2. Execution Summary", 20, 12);

                addText(doc, String.format(
                        "This run holds %d test cases, of which %d were executed. Of those, %d%% passed. The results below summarize the outcome.",
                        summary.total(), summary.executed(), summary.passRate()),
                        ReportFont.LEAD.ptRounded(), false, BLACK, NO_BORDER, 12);

                // Seven tiles when the run has removed cases, six otherwise:
                // the total counts them, so without a tile of their own the
                // figures below the total do not add up to it.
                final int tiles = summary.hasRemoved() ? 7 : 6;

                XWPFTable statsTable = doc.createTable(1, tiles);
                statsTable.setWidth("100%");
                statsTable.setWidthType(TableWidthType.PCT);
                setTableBorders(statsTable);
                setTableWidths(statsTable, evenWidths(tiles));

                int tile = 0;
                addStatCell(statsTable, tile++, String.valueOf(summary.total()), "Total Cases", DARK_NAVY);
                addStatCell(statsTable, tile++, String.valueOf(summary.passed()), TestStatus.PASSED.getLabel(), GREEN);
                addStatCell(statsTable, tile++, String.valueOf(summary.failed()), TestStatus.FAILED.getLabel(), RED);
                addStatCell(statsTable, tile++, String.valueOf(summary.blocked()), TestStatus.BLOCKED.getLabel(), DARK_YELLOW);
                addStatCell(statsTable, tile++, String.valueOf(summary.untested()), TestStatus.UNTESTED.getLabel(), DARK_GRAY);
                if (summary.hasRemoved()) {
                    addStatCell(statsTable, tile++, String.valueOf(summary.removed()), TestStatus.REMOVED.getLabel(), DARK_GRAY);
                }
                addStatCell(statsTable, tile, summary.passRate() + "%", "Pass Rate", MEDIUM_BLUE);

                // Only what the tester wrote - see the PDF generator.
                final boolean analysed = ResultAnalysis.anyWrittenIn(tr.getResultAnalysis());

                if (analysed) {
                    addHeading(doc, "3. Result Analysis", 20, 12);

                    for (final ResultAnalysis section : ResultAnalysis.values()) {
                        final @NotNull String written = section.writtenIn(tr.getResultAnalysis());
                        if (written.isEmpty()) continue;

                        addColoredCount(doc, section.heading(summary), section.getHexColor());
                        addText(doc, written, ReportFont.BODY.ptRounded(), false, BLACK, NO_BORDER, 8);
                    }
                }

                // One case table per status, empty ones omitted, numbered as
                // printed so an absent section leaves no gap in the numbering.
                int sectionNumber = analysed ? 4 : 3;
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
        // Several lines stay several lines - the result analysis is written in
        // paragraphs and came out as one sentence here while the PDF showed it
        // as typed.
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

    /**
     * One colored count in the result analysis. It carries the spacing the body
     * paragraph below it used to provide.
     */
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
        addCaseHeader(headerRow, 1, "Test Case", headerBg, headerFg);
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
                XWPFParagraph ap = tcCell.addParagraph();
                XWPFRun arun = ap.createRun();
                arun.setText("Actual result: " + actualResult);
                arun.setFontSize(ReportFont.SMALL.ptRounded());
                arun.setFontFamily("Calibri");
                arun.setColor(DARK_GRAY);

                // Only when there is one, and monospaced: a stacktrace read in
                // a proportional font loses the indentation that makes it
                // scannable.
                if (!item.getStacktrace().isBlank()) {
                    final XWPFRun trace = tcCell.addParagraph().createRun();
                    trace.setText(item.getStacktrace());
                    trace.setFontSize(ReportFont.SMALL.ptRounded());
                    trace.setFontFamily("Consolas");
                    trace.setColor(DARK_GRAY);
                }
            }

            if (withFailureDetail) {
                XWPFTableCell priCell = row.getCell(2);
                shadeCell(priCell, rowBg);
                setCellPadding(priCell, 4, 6, 4, 6);
                BugPriority pri = item.getBugPriority();
                String priColor = pri.getEmphasis().getHexColor();
                setCellText(priCell, pri.getName(), ReportFont.BODY.ptRounded(), true, priColor);
            }

            if (withFailureDetail) {
                XWPFTableCell sevCell = row.getCell(3);
                shadeCell(sevCell, rowBg);
                setCellPadding(sevCell, 4, 6, 4, 6);
                BugSeverity sev = item.getBugSeverity();
                String sevColor = sev.getEmphasis().getHexColor();
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "—";
                setCellText(sevCell, sevText, ReportFont.BODY.ptRounded(), true, sevColor);
            }

            idx++;
        }

        // Must run after the data rows are created — setTableBorders iterates existing rows,
        // so calling it right after createTable left every data row borderless.
        setTableBorders(table);
        autoFitToContent(table);
    }

    /**
     * Lets Word size the columns from what is in them.
     * <p>
     * The widths used to be a fixed share of the page, which meant guessing how
     * much room "Enhancement" needs: too little and it wrapped, too much and the
     * description column gave up space for nothing on every row of every report.
     * The PDF and the HTML report both size these tables from their content now,
     * and this is Word's way of doing it - the layout is marked autofit and each
     * cell asks for no particular width, so Word measures the text itself.
     * <p>
     * The table still fills the page: its own width stays a full percentage, so
     * what autofit decides is the split between the columns, not how much of the
     * page they use.
     */
    private void autoFitToContent(final @NotNull XWPFTable table) {
        final @NotNull CTTblPr properties = table.getCTTbl().getTblPr();
        final @NotNull CTTblLayoutType layout =
                properties.isSetTblLayout() ? properties.getTblLayout() : properties.addNewTblLayout();
        layout.setType(STTblLayoutType.AUTOFIT);

        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final @NotNull CTTcPr cellProperties = getTcPr(cell);
                // Reused when the cell already has one: adding a second <w:tcW>
                // is invalid XML, and Word answers invalid XML by refusing to
                // open the file rather than by ignoring the extra element.
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

    /**
     * Writes text that may be several lines, as several lines.
     * <p>
     * Word has no line break inside a run unless one is asked for: the whole
     * string went in as one piece, so a change log covering three stories came
     * out as one sentence with the breaks silently dropped. The PDF had always
     * shown them.
     *
     * @param replaceFirst overwrite the run's existing first piece rather than
     *                     adding to it, for a cell being filled a second time
     */
    private void writeLines(final @NotNull XWPFRun run, final @NotNull String text, final boolean replaceFirst) {
        // lines() splits on every line terminator without this file having to
        // name one, and answers nothing at all for empty text - which still
        // needs a piece written, or the cell keeps whatever was there before.
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

    /**
     * Percentages that fill the row exactly, whatever the tile count. The
     * leftmost columns carry the remainder, which is what the handwritten
     * widths did when there were always six of them.
     */
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
        footerRun(p.createRun(), date + "  |  Generated automatically by ", DARK_GRAY);

        // The plugin's name is a link here too. The PDF and the HTML report both
        // linked it and this one printed it as plain text, so the one format a
        // reader is most likely to have open was the one they could not click.
        final @NotNull XWPFHyperlinkRun link = p.createHyperlinkRun(ReportText.PLUGIN_URL);
        footerRun(link, "Testin", LINK_BLUE);
        link.setUnderline(UnderlinePatterns.SINGLE);

        footerRun(p.createRun(), " IntelliJ plugin.", DARK_GRAY);
    }

    /**
     * One piece of the footer line, so the three of them cannot drift in size or
     * face while only their color differs.
     */
    private void footerRun(final @NotNull XWPFRun run, final @NotNull String text, final @NotNull String color) {
        run.setText(text);
        run.setFontSize(ReportFont.CAPTION.ptRounded());
        run.setFontFamily("Calibri");
        run.setColor(color);
    }

    private void applyPageMargins(final @NotNull XWPFDocument doc) {
        final @NotNull CTBody body = doc.getDocument().getBody();
        final @NotNull CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        final @NotNull CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();

        // Half an inch at the sides, an inch top and bottom. A report is a wide
        // table under a heading, and an inch of paper down each edge was room the
        // test case column wanted. Left and right are the same number on purpose:
        // they were both an inch before, and before that only the left was set at
        // all, from a right margin nothing had initialized.
        final long sideTwips = 720L;
        final long endTwips = 1440L;

        pgMar.setLeft(sideTwips);
        pgMar.setRight(sideTwips);
        pgMar.setTop(endTwips);
        pgMar.setBottom(endTwips);
    }

}
