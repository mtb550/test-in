package org.testin.report.generators;

import com.intellij.openapi.project.Project;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.testin.logger.Logger;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.TestRunConfiguration;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class TestRunWordGenerator {

    private static final Map<Integer, int[]> COLUMN_WIDTHS = Map.of(
            2, new int[]{3, 97},
            3, new int[]{3, 87, 10},
            4, new int[]{3, 77, 10, 10}
    );
    /**
     * No rule under the paragraph, said as a color of no color rather than as a
     * null the border writer would have to check (#71).
     */
    final String NO_BORDER = "";

    final String DARK_NAVY = "1F3864";
    final String MEDIUM_BLUE = "2E5496";
    final String DARK_GRAY = "595959";
    final String GREEN = "2E7D32";
    final String RED = "C0392B";
    final String DARK_YELLOW = "B8860B";
    final String LIGHT_BG = "F2F5FA";
    final String BORDER_GRAY = "D0D7E5";
    final String WHITE = "FFFFFF";
    final String BLACK = "000000";
    private final Map<BugPriority, String> PRIORITY_COLOR = Map.of(
            BugPriority.HIGH, RED,
            BugPriority.MEDIUM, DARK_YELLOW
    );
    private final Map<BugSeverity, String> SEVERITY_COLOR = Map.of(
            BugSeverity.BLOCKER, RED,
            BugSeverity.MAJOR, DARK_YELLOW
    );

    public byte @NotNull [] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                                     final @NotNull TestRunDto tr,
                                     final @NotNull Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {

                final String projectName = Services.getInstance(p, BoundTestProject.class).name();

                addText(doc, "TEST SUMMARY REPORT", 18, true, DARK_NAVY, NO_BORDER, 2);

                String subtitleText = projectName + "  |  " + tr.getPlatform() + ", " + tr.getComponent();
                addText(doc, subtitleText, 10, false, MEDIUM_BLUE, DARK_NAVY, 5);

                XWPFParagraph conf = addText(doc, "Confidential — QA Test Execution Summary", 8, false, DARK_GRAY, NO_BORDER, 20);
                setItalic(conf);

                addHeading(doc, "1. Report Overview", 0, 15);

                // One traversal of the results serves the whole report: the
                // counts below, the pass rate, and who executed it.
                final TestRunSummary summary = TestRunSummary.of(tr.getResults());

                XWPFTable overviewTable = doc.createTable(1, 2);
                overviewTable.setWidth("100%");
                overviewTable.setWidthType(TableWidthType.PCT);
                setTableWidths(overviewTable, 30, 70);

                addOverviewRow(overviewTable, 0, "Project", projectName);

                if (!tr.getChangeLog().isEmpty())
                    addOverviewRow(overviewTable, 1, TestRunConfiguration.CHANGE_LOG.getDisplayName(), tr.getChangeLog());

                addOverviewRow(overviewTable, 2, TestRunConfiguration.COMMIT_ID.getDisplayName(), tr.getCommitId().isEmpty() ? "n/a" : tr.getCommitId());

                if (!tr.getPlatform().isEmpty() || !tr.getComponent().isEmpty())
                    addOverviewRow(overviewTable, 3, TestRunConfiguration.PLATFORM.getDisplayName() + " \\ " + TestRunConfiguration.COMPONENT.getDisplayName(), tr.getPlatform() + ", " + tr.getComponent());

                if (!tr.getTestType().isEmpty())
                    addOverviewRow(overviewTable, 4, TestRunConfiguration.TEST_TYPE.getDisplayName(), tr.getTestType());

                addOverviewRow(overviewTable, 5, "Executed By", summary.executedBy());
                addOverviewRow(overviewTable, 6, "Execution Started", Display.formatDate(tr.getExecutionStartedAt()));
                addOverviewRow(overviewTable, 7, "Execution Ended", Display.formatDate(tr.getExecutionEndedAt()));
                addOverviewRow(overviewTable, 8, "Run Status", trDir.getMarker().getStatus().name());


                setTableBorders(overviewTable);

                addHeading(doc, "2. Execution Summary", 20, 12);

                addText(doc, String.format(
                        "This run holds %d test cases, of which %d were executed. Of those, %d%% passed. The results below summarize the outcome.",
                        summary.total(), summary.executed(), summary.passRate()), 11, false, BLACK, null, 12);

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
                addStatCell(statsTable, tile++, String.valueOf(summary.passed()), "Passed", GREEN);
                addStatCell(statsTable, tile++, String.valueOf(summary.failed()), "Failed", RED);
                addStatCell(statsTable, tile++, String.valueOf(summary.blocked()), "Blocked", DARK_YELLOW);
                addStatCell(statsTable, tile++, String.valueOf(summary.untested()), "Untested", DARK_GRAY);
                if (summary.hasRemoved()) {
                    addStatCell(statsTable, tile++, String.valueOf(summary.removed()), "Removed", DARK_GRAY);
                }
                addStatCell(statsTable, tile, summary.passRate() + "%", "Pass Rate", MEDIUM_BLUE);

                addHeading(doc, "3. Result Analysis", 20, 12);
                addColoredCount(doc, "Passed (" + summary.passed() + ")", GREEN);
                addColoredCount(doc, "Failed (" + summary.failed() + ")", RED);
                addColoredCount(doc, "Blocked (" + summary.blocked() + ")", DARK_YELLOW);
                addColoredCount(doc, "Untested (" + summary.untested() + ")", DARK_GRAY);
                if (summary.hasRemoved()) {
                    addColoredCount(doc, "Removed (" + summary.removed() + ")", DARK_GRAY);
                }

                // One case table per status, empty ones omitted, numbered as
                // printed so an absent section leaves no gap in the numbering.
                int sectionNumber = 4;
                for (final ReportSection section : ReportSection.values()) {
                    final long count = section.count(summary);
                    if (count == 0) continue;

                    buildCaseTable(doc, String.valueOf(sectionNumber++), section.getTitle(),
                            section.description(String.valueOf(count)), tr, detailsMap, colorOf(section),
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

    private @NotNull XWPFParagraph addText(final @NotNull XWPFDocument doc, final @NotNull String text, final int size,
                                           final boolean bold, final @NotNull String color,
                                           final @NotNull String bottomBorder, final int spacingAfterPt) {
        final XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfterPt * 20);
        final XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
        run.setBold(bold);
        run.setColor(color);
        if (!bottomBorder.isEmpty()) {
            final CTBorder bottom = p.getCTPPr().addNewPBdr().addNewBottom();
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

    private void addHeading(final @NotNull XWPFDocument doc, final @NotNull String text, final int beforePt,
                            final int afterPt) {
        final XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(beforePt * 20);
        p.setSpacingAfter(afterPt * 20);
        final XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(13);
        run.setFontFamily("Calibri");
        run.setBold(true);
        run.setColor(DARK_NAVY);
        final CTBorder headingBottom = p.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(BigInteger.valueOf(8));
        headingBottom.setColor(DARK_NAVY);
    }

    private void addOverviewRow(final @NotNull XWPFTable table, final int rowIdx, final @NotNull String label,
                                final @NotNull String value) {
        final XWPFTableRow row = rowIdx == 0 ? table.getRow(0) : table.createRow();
        final XWPFTableCell labelCell = row.getCell(0);
        final XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, LIGHT_BG);
        setCellPadding(labelCell, 4, 8, 4, 8);
        setCellPadding(valueCell, 4, 8, 4, 8);
        setCellText(labelCell, label, 10, true, DARK_NAVY);
        setCellText(valueCell, value, 10, false, BLACK);
    }

    private void addStatCell(final @NotNull XWPFTable table, final int col, final @NotNull String number,
                             final @NotNull String label, final @NotNull String numberColor) {
        final XWPFTableRow row = table.getRow(0);
        final XWPFTableCell cell = row.getCell(col);
        shadeCell(cell, LIGHT_BG);
        setCellPadding(cell, 8, 6, 8, 6);
        setCellText(cell, number, 20, true, numberColor);

        cell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

        final XWPFParagraph lp = cell.addParagraph();
        lp.setAlignment(ParagraphAlignment.CENTER);
        lp.setSpacingBefore(80);
        final XWPFRun lrun = lp.createRun();
        lrun.setText(label);
        lrun.setFontSize(9);
        lrun.setFontFamily("Calibri");
        lrun.setBold(true);
        lrun.setColor(DARK_GRAY);
    }

    /**
     * One colored count in the result analysis. It carries the spacing the body
     * paragraph below it used to provide.
     */
    private void addColoredCount(final @NotNull XWPFDocument doc, final @NotNull String heading,
                                 final @NotNull String headingColor) {
        final XWPFParagraph hp = doc.createParagraph();
        hp.setSpacingAfter(120);

        final XWPFRun hrun = hp.createRun();
        hrun.setText(heading);
        hrun.setFontSize(11);
        hrun.setFontFamily("Calibri");
        hrun.setBold(true);
        hrun.setColor(headingColor);
    }

    /**
     * The header color of a section's table. Per format, because Word wants a hex
     * string where the PDF wants a DeviceRgb, and a shared section definition has
     * no business knowing about either.
     */
    private @NotNull String colorOf(final @NotNull ReportSection section) {
        return switch (section) {
            case FAILED -> RED;
            case PASSED -> GREEN;
            case BLOCKED -> DARK_YELLOW;
            case UNTESTED, REMOVED -> DARK_GRAY;
        };
    }

    private void buildCaseTable(final @NotNull XWPFDocument doc, final @NotNull String sectionNumber,
                                final @NotNull String sectionTitle, final @NotNull String description,
                                final @NotNull TestRunDto tr,
                                final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull String headerBg,
                                final boolean withFailureDetail, final @NotNull Predicate<TestRunItems> filter) {
        addHeading(doc, sectionNumber + ". " + sectionTitle, 20, 12);
        addText(doc, description, 11, false, BLACK, NO_BORDER, 12);

        int cols = withFailureDetail ? 4 : 2;
        XWPFTable table = doc.createTable(1, cols);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        XWPFTableRow headerRow = table.getRow(0);
        addCaseHeader(headerRow, 0, "#", headerBg);
        addCaseHeader(headerRow, 1, "Test Case", headerBg);
        if (withFailureDetail) addCaseHeader(headerRow, 2, "Priority", headerBg);
        if (withFailureDetail) addCaseHeader(headerRow, 3, "Severity", headerBg);

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
            setCellText(numCell, String.valueOf(idx), 9, false, DARK_GRAY);
            numCell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

            XWPFTableCell tcCell = row.getCell(1);
            shadeCell(tcCell, rowBg);
            setCellPadding(tcCell, 4, 6, 4, 6);
            final TestCaseDto tc = detailsMap.get(item.getId());
            String tcName = tc != null ? tc.getDescription() : "";
            if (tcName.isEmpty()) tcName = "—";
            setCellText(tcCell, tcName, 9, false, BLACK);

            if (withFailureDetail) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                XWPFParagraph ap = tcCell.addParagraph();
                XWPFRun arun = ap.createRun();
                arun.setText("Actual result: " + actualResult);
                arun.setFontSize(8);
                arun.setFontFamily("Calibri");
                arun.setColor(DARK_GRAY);
            }

            if (withFailureDetail) {
                XWPFTableCell priCell = row.getCell(2);
                shadeCell(priCell, rowBg);
                setCellPadding(priCell, 4, 6, 4, 6);
                BugPriority pri = item.getBugPriority();
                String priColor = PRIORITY_COLOR.getOrDefault(pri, DARK_GRAY);
                setCellText(priCell, pri.getName(), 9, true, priColor);
            }

            if (withFailureDetail) {
                XWPFTableCell sevCell = row.getCell(3);
                shadeCell(sevCell, rowBg);
                setCellPadding(sevCell, 4, 6, 4, 6);
                BugSeverity sev = item.getBugSeverity();
                String sevColor = SEVERITY_COLOR.getOrDefault(sev, DARK_GRAY);
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "—";
                setCellText(sevCell, sevText, 9, true, sevColor);
            }

            idx++;
        }

        // Must run after the data rows are created — setTableBorders iterates existing rows,
        // so calling it right after createTable left every data row borderless.
        setTableBorders(table);
        setTableWidths(table, widthsFor(cols));
    }

    private void addCaseHeader(final @NotNull XWPFTableRow headerRow, final int col, final @NotNull String text,
                               final @NotNull String bgColor) {
        final XWPFTableCell cell = headerRow.getCell(col);
        shadeCell(cell, bgColor);
        setCellPadding(cell, 5, 6, 5, 6);
        setCellText(cell, text, 9, true, WHITE);
    }

    private void setCellText(final @NotNull XWPFTableCell cell, final @NotNull String text, final int size,
                             final boolean bold, final @NotNull String color) {
        final XWPFParagraph p = cell.getParagraphs().getFirst();
        if (p.getRuns().isEmpty()) {
            final XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);

        } else {
            final XWPFRun run = p.getRuns().getFirst();
            run.setText(text, 0);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);
        }
    }

    private void shadeCell(final @NotNull XWPFTableCell cell, final @NotNull String hex) {
        getTcPr(cell).addNewShd().setFill(hex);
    }

    private void setCellPadding(final @NotNull XWPFTableCell cell, final int topPt, final int leftPt,
                                final int bottomPt, final int rightPt) {
        final CTTcMar mar = getTcPr(cell).addNewTcMar();
        final CTTblWidth top = mar.addNewTop();
        top.setW(topPt * 20);
        top.setType(STTblWidth.Enum.forString("dxa"));
        final CTTblWidth left = mar.addNewLeft();
        left.setW(leftPt * 20);
        left.setType(STTblWidth.Enum.forString("dxa"));
        final CTTblWidth bottom = mar.addNewBottom();
        bottom.setW(bottomPt * 20);
        bottom.setType(STTblWidth.Enum.forString("dxa"));
        final CTTblWidth right = mar.addNewRight();
        right.setW(rightPt * 20);
        right.setType(STTblWidth.Enum.forString("dxa"));
    }

    private @NotNull CTTcPr getTcPr(final @NotNull XWPFTableCell cell) {
        final CTTc ct = cell.getCTTc();
        return ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
    }

    private void setTableBorders(final @NotNull XWPFTable table) {
        for (final XWPFTableRow row : table.getRows()) {
            for (final XWPFTableCell cell : row.getTableCells()) {
                final CTTcBorders borders = getTcPr(cell).addNewTcBorders();
                final CTBorder top = borders.addNewTop();
                top.setColor(BORDER_GRAY);
                top.setSz(BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                final CTBorder bottom = borders.addNewBottom();
                bottom.setColor(BORDER_GRAY);
                bottom.setSz(BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                final CTBorder left = borders.addNewLeft();
                left.setColor(BORDER_GRAY);
                left.setSz(BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                final CTBorder right = borders.addNewRight();
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
        final XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            getTcPr(row.getCell(i)).addNewTcW().setW(percents[i] * 100);
            getTcPr(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(final @NotNull XWPFDocument doc, final @NotNull String date) {
        final XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        final XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        final XWPFRun run = p.createRun();
        run.setText(date + "  |  Generated automatically by Testin IntelliJ plugin.");
        run.setFontSize(8);
        run.setFontFamily("Calibri");
        run.setColor(DARK_GRAY);
    }

    private void applyPageMargins(final @NotNull XWPFDocument doc) {
        final CTBody body = doc.getDocument().getBody();
        final CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        final CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();

        // Standard 1-inch margins on all four sides. The old code only ever set
        // the left margin, copied from a right margin that was never initialized.
        final long marginTwips = 1440L;
        pgMar.setLeft(marginTwips);
        pgMar.setRight(marginTwips);
        pgMar.setTop(marginTwips);
        pgMar.setBottom(marginTwips);
    }

    private int @NotNull [] widthsFor(final int cols) {
        return COLUMN_WIDTHS.getOrDefault(cols, new int[]{3, 77, 10, 10});
    }
}
