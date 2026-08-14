package org.testin.generateReport.generators;

import com.intellij.openapi.project.Project;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.testin.enums.BugPriority;
import org.testin.enums.BugSeverity;
import org.testin.enums.TestRunConfiguration;
import org.testin.enums.TestStatus;
import org.testin.logger.Logger;
import org.testin.mappers.Config;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.services.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TestRunWordGenerator {

    private static final Map<Integer, int[]> COLUMN_WIDTHS = Map.of(
            2, new int[]{3, 97},
            3, new int[]{3, 87, 10},
            4, new int[]{3, 77, 10, 10}
    );
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

                String projectName = "";
                TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto) Services.getInstance(p, ProjectPanel.class)
                        .getTestProjectSelector().getSelectedTestProject().getSelectedItem();
                if (selectedProject != null) {
                    projectName = selectedProject.getName();
                }

                addText(doc, "TEST SUMMARY REPORT", 18, true, DARK_NAVY, null, 2);

                String subtitleText = projectName + "  |  " + tr.getPlatform() + ", " + tr.getComponent();
                addText(doc, subtitleText, 10, false, MEDIUM_BLUE, DARK_NAVY, 5);

                XWPFParagraph conf = addText(doc, "Confidential — QA Test Execution Summary", 8, false, DARK_GRAY, null, 20);
                setItalic(conf);

                addHeading(doc, "1. Report Overview", 0, 15);

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

                String executedByAll = tr.getResults().stream()
                        .map(TestRunItems::getExecutedBy)
                        .filter(s -> !s.trim().isEmpty())
                        .distinct()
                        .collect(Collectors.joining(", "));
                addOverviewRow(overviewTable, 5, "Executed By", executedByAll);

                addOverviewRow(overviewTable, 6, "Execution Date", tr.getCreatedAt().format(Config.getDateFormatterPattern()));
                addOverviewRow(overviewTable, 7, "Run Status", trDir.getMarker().getStatus().name());


                setTableBorders(overviewTable);

                addHeading(doc, "2. Execution Summary", 20, 12);

                long total = tr.getResults().size();
                // One traversal counts every status; a new TestStatus constant is
                // included automatically instead of needing another filter pass.
                final TestRunSummary summary = TestRunSummary.of(tr.getResults());
                long passed = summary.passed();
                long failed = summary.failed();
                long blocked = summary.blocked();
                long pending = summary.pending();
                long passRate = summary.passRate();

                addText(doc, String.format(
                        "A total of %d test cases were executed for this run. The run completed with a %d%% pass rate. The results below summarize the outcome across all executed cases.",
                        total, passRate), 11, false, BLACK, null, 12);

                XWPFTable statsTable = doc.createTable(1, 5);
                statsTable.setWidth("100%");
                statsTable.setWidthType(TableWidthType.PCT);
                setTableBorders(statsTable);
                setTableWidths(statsTable, 20, 20, 20, 20, 20);

                addStatCell(statsTable, 0, String.valueOf(total), "Total Cases", DARK_NAVY);
                addStatCell(statsTable, 1, String.valueOf(passed), "Passed", GREEN);
                addStatCell(statsTable, 2, String.valueOf(failed), "Failed", RED);
                addStatCell(statsTable, 3, String.valueOf(pending + blocked), "Blocked", DARK_YELLOW);
                addStatCell(statsTable, 4, passRate + "%", "Pass Rate", MEDIUM_BLUE);

                addHeading(doc, "3. Result Analysis", 20, 12);
                addColoredBody(doc, "Passed (" + passed + "): ", GREEN, "n\\a");
                addColoredBody(doc, "Failed (" + failed + "): ", RED, "n\\a");
                addColoredBody(doc, "Pending (" + (pending + blocked) + "): ", DARK_YELLOW, "n\\a");

                if (failed > 0) {
                    buildCaseTable(doc, "4", "Failed Test Cases",
                            "The following %d cases failed and require remediation. Real-user identification validation is the primary defect cluster.",
                            failed, tr, detailsMap, RED, true, true, true,
                            item -> item.getStatus() == TestStatus.FAILED);
                }

                if (passed > 0) {
                    buildCaseTable(doc, "5", "Passed Test Cases",
                            "The following %d cases passed validation and behaved as expected across all verification points.",
                            passed, tr, detailsMap, GREEN, false, false, false,
                            item -> item.getStatus() == TestStatus.PASSED);
                }

                long pendingTotal = pending + blocked;
                if (pendingTotal > 0) {
                    buildCaseTable(doc, "6", "Pending Test Cases",
                            "The following %d cases were not executed in this cycle, primarily blocked by environment/data dependencies. They are carried forward to the next run.",
                            pendingTotal, tr, detailsMap, DARK_YELLOW, false, false, false,
                            item -> item.getStatus() == TestStatus.PENDING || item.getStatus() == TestStatus.UNTESTED || item.getStatus() == TestStatus.BLOCKED);
                }

                addFooter(doc, ZonedDateTime.now().format(Config.getDateFormatterPattern()));

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
                                           final @Nullable String bottomBorder, final int spacingAfterPt) {
        final XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfterPt * 20);
        final XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
        run.setBold(bold);
        run.setColor(color);
        if (bottomBorder != null) {
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

    private void addColoredBody(final @NotNull XWPFDocument doc, final @NotNull String heading,
                                final @NotNull String headingColor, final @NotNull String body) {
        final XWPFParagraph hp = doc.createParagraph();
        hp.setSpacingAfter(0);
        final XWPFRun hrun = hp.createRun();
        hrun.setText(heading);
        hrun.setFontSize(11);
        hrun.setFontFamily("Calibri");
        hrun.setBold(true);
        hrun.setColor(headingColor);

        final XWPFParagraph bp = doc.createParagraph();
        bp.setSpacingAfter(120);
        final XWPFRun brun = bp.createRun();
        brun.setText(body);
        brun.setFontSize(11);
        brun.setFontFamily("Calibri");
        brun.setColor(BLACK);
    }

    private void buildCaseTable(final @NotNull XWPFDocument doc, final @NotNull String sectionNumber,
                                final @NotNull String sectionTitle, final @NotNull String descriptionFmt,
                                final long count, final @NotNull TestRunDto tr,
                                final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull String headerBg,
                                final boolean withPriority, final boolean withSeverity,
                                final boolean withActualResult, final @NotNull Predicate<TestRunItems> filter) {
        addHeading(doc, sectionNumber + ". " + sectionTitle, 20, 12);
        addText(doc, String.format(descriptionFmt, count), 11, false, BLACK, null, 12);

        int cols = 1 + 1 + (withPriority ? 1 : 0) + (withSeverity ? 1 : 0);
        XWPFTable table = doc.createTable(1, cols);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);

        XWPFTableRow headerRow = table.getRow(0);
        addCaseHeader(headerRow, 0, "#", headerBg);
        addCaseHeader(headerRow, 1, "Test Case", headerBg);
        if (withPriority) addCaseHeader(headerRow, 2, "Priority", headerBg);
        if (withSeverity) addCaseHeader(headerRow, 3, "Severity", headerBg);

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

            if (withActualResult) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "—";
                XWPFParagraph ap = tcCell.addParagraph();
                XWPFRun arun = ap.createRun();
                arun.setText("Actual result: " + actualResult);
                arun.setFontSize(8);
                arun.setFontFamily("Calibri");
                arun.setColor(DARK_GRAY);
            }

            if (withPriority) {
                XWPFTableCell priCell = row.getCell(2);
                shadeCell(priCell, rowBg);
                setCellPadding(priCell, 4, 6, 4, 6);
                BugPriority pri = item.getBugPriority();
                String priColor = PRIORITY_COLOR.getOrDefault(pri, DARK_GRAY);
                setCellText(priCell, pri.getName(), 9, true, priColor);
            }

            if (withSeverity) {
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
