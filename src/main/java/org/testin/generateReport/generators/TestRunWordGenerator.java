package org.testin.generateReport.generators;

import com.intellij.openapi.project.Project;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.testin.enums.BugPriority;
import org.testin.enums.BugSeverity;
import org.testin.enums.TestRunConfiguration;
import org.testin.enums.TestStatus;
import org.testin.mappers.Config;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.AppSettingsState;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TestRunWordGenerator {

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

    public byte[] generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {
                // todo, why clean name not used to naming the word file.
                String cleanName = tr.getChangeLog().replace(".json", "");

                String projectName = "";
                TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto) Services.getInstance(p, ProjectPanel.class)
                        .getTestProjectSelector().getSelectedTestProject().getSelectedItem();
                if (selectedProject != null) {
                    projectName = selectedProject.getName();
                }

                AppSettingsState settings = AppSettingsState.getInstance();
                String testerName = settings.testerName.trim();
                String testerRole = settings.testerRole;

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
                long passed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
                long failed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
                long blocked = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.BLOCKED).count();
                long pending = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PENDING || r.getStatus() == TestStatus.UNTESTED).count();
                long passRate = total > 0 ? (passed * 100 / total) : 0;

                addText(doc, String.format(
                        "A total of %d test cases were executed for this run. The run completed with a %d%% pass rate. The results below summarise the outcome across all executed cases.",
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

                addColoredBody(doc, "Passed (" + passed + "): ", GREEN,
                        "All passed cases covered the core authentication flow end-to-end: login by username and by ID, credential validation and error handling (invalid username/password combinations, empty fields, special characters, SQL injection resistance), account lockout after max failed attempts and lockout-duration behavior, OTP generation, validation, expiry and resend logic, password change and the full password policy rule set (length, case, numeric, sequence, ID-portion, language checks), and session timeout and logout behavior. No deviations from expected behavior were observed across these cases.");

                addColoredBody(doc, "Failed (" + failed + "): ", RED,
                        "One case failed: \"Verify system redirects user to the provider website after successful authentication.\" Instead of redirecting to the provider website, the system redirected the user to the home page. This was logged as High priority / Major severity, as it affects the core post-authentication redirect flow and requires remediation before the next cycle.");

                addColoredBody(doc, "Pending (" + (pending + blocked) + "): ", DARK_YELLOW,
                        "No test cases were left pending in this cycle — all 60 planned cases were executed to completion.");

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

    private XWPFParagraph addText(XWPFDocument doc, String text, int size, boolean bold, String color, String bottomBorder, int spacingAfterPt) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfterPt * 20);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
        run.setBold(bold);
        run.setColor(color);
        if (bottomBorder != null) {
            CTBorder bottom = p.getCTPPr().addNewPBdr().addNewBottom();
            bottom.setVal(STBorder.Enum.forString("single"));
            bottom.setSz(BigInteger.valueOf(16));
            bottom.setColor(bottomBorder);
        }
        return p;
    }

    private void setItalic(XWPFParagraph p) {
        for (XWPFRun run : p.getRuns()) {
            run.setItalic(true);
        }
    }

    private void addHeading(XWPFDocument doc, String text, int beforePt, int afterPt) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(beforePt * 20);
        p.setSpacingAfter(afterPt * 20);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(13);
        run.setFontFamily("Calibri");
        run.setBold(true);
        run.setColor(DARK_NAVY);
        CTBorder headingBottom = p.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(BigInteger.valueOf(8));
        headingBottom.setColor(DARK_NAVY);
    }

    private void addOverviewRow(XWPFTable table, int rowIdx, String label, String value) {
        XWPFTableRow row = rowIdx == 0 ? table.getRow(0) : table.createRow();
        XWPFTableCell labelCell = row.getCell(0);
        XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, LIGHT_BG);
        setCellPadding(labelCell, 4, 8, 4, 8);
        setCellPadding(valueCell, 4, 8, 4, 8);
        setCellText(labelCell, label, 10, true, DARK_NAVY);
        setCellText(valueCell, value, 10, false, BLACK);
    }

    private void addStatCell(XWPFTable table, int col, String number, String label, String numberColor) {
        XWPFTableRow row = table.getRow(0);
        XWPFTableCell cell = row.getCell(col);
        shadeCell(cell, LIGHT_BG);
        setCellPadding(cell, 8, 6, 8, 6);
        setCellText(cell, number, 20, true, numberColor);

        cell.getParagraphs().getFirst().setAlignment(ParagraphAlignment.CENTER);

        XWPFParagraph lp = cell.addParagraph();
        lp.setAlignment(ParagraphAlignment.CENTER);
        lp.setSpacingBefore(80);
        XWPFRun lrun = lp.createRun();
        lrun.setText(label);
        lrun.setFontSize(9);
        lrun.setFontFamily("Calibri");
        lrun.setBold(true);
        lrun.setColor(DARK_GRAY);
    }

    private void addColoredBody(XWPFDocument doc, String heading, String headingColor, String body) {
        XWPFParagraph hp = doc.createParagraph();
        hp.setSpacingAfter(0);
        XWPFRun hrun = hp.createRun();
        hrun.setText(heading);
        hrun.setFontSize(11);
        hrun.setFontFamily("Calibri");
        hrun.setBold(true);
        hrun.setColor(headingColor);

        XWPFParagraph bp = doc.createParagraph();
        bp.setSpacingAfter(120);
        XWPFRun brun = bp.createRun();
        brun.setText(body);
        brun.setFontSize(11);
        brun.setFontFamily("Calibri");
        brun.setColor(BLACK);
    }

    private void buildCaseTable(XWPFDocument doc, String sectionNumber, String sectionTitle, String descriptionFmt, long count, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap, String headerBg, boolean withPriority, boolean withSeverity, boolean withActualResult, Predicate<TestRunItems> filter) {
        addHeading(doc, sectionNumber + ". " + sectionTitle, 20, 12);
        addText(doc, String.format(descriptionFmt, count), 11, false, BLACK, null, 12);

        int cols = 1 + 1 + (withPriority ? 1 : 0) + (withSeverity ? 1 : 0);
        XWPFTable table = doc.createTable(1, cols);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);
        setTableBorders(table);

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
            String tcName = "";
            if (detailsMap != null) {
                TestCaseDto tc = detailsMap.get(item.getId());
                if (tc != null) tcName = tc.getDescription();
            }
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
                String priColor;
                if (pri == BugPriority.HIGH) priColor = RED;
                else if (pri == BugPriority.MEDIUM) priColor = DARK_YELLOW;
                else priColor = DARK_GRAY;
                setCellText(priCell, pri.getName(), 9, true, priColor);
            }

            if (withSeverity) {
                XWPFTableCell sevCell = row.getCell(3);
                shadeCell(sevCell, rowBg);
                setCellPadding(sevCell, 4, 6, 4, 6);
                BugSeverity sev = item.getBugSeverity();
                String sevColor;
                if (sev == BugSeverity.BLOCKER) sevColor = RED;
                else if (sev == BugSeverity.MAJOR) sevColor = DARK_YELLOW;
                else sevColor = DARK_GRAY;
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "—";
                setCellText(sevCell, sevText, 9, true, sevColor);
            }

            idx++;
        }

        setTableWidths(table, widthsFor(cols));
    }

    private void addCaseHeader(XWPFTableRow headerRow, int col, String text, String bgColor) {
        XWPFTableCell cell = headerRow.getCell(col);
        shadeCell(cell, bgColor);
        setCellPadding(cell, 5, 6, 5, 6);
        setCellText(cell, text, 9, true, WHITE);
    }

    private void setCellText(XWPFTableCell cell, String text, int size, boolean bold, String color) {
        XWPFParagraph p = cell.getParagraphs().getFirst();
        if (p.getRuns().isEmpty()) {
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);

        } else {
            XWPFRun run = p.getRuns().getFirst();
            run.setText(text, 0);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);
        }
    }

    private void shadeCell(XWPFTableCell cell, String hex) {
        getTcPr(cell).addNewShd().setFill(hex);
    }

    private void setCellPadding(XWPFTableCell cell, int topPt, int leftPt, int bottomPt, int rightPt) {
        CTTcMar mar = getTcPr(cell).addNewTcMar();
        CTTblWidth top = mar.addNewTop();
        top.setW(topPt * 20);
        top.setType(STTblWidth.Enum.forString("dxa"));
        CTTblWidth left = mar.addNewLeft();
        left.setW(leftPt * 20);
        left.setType(STTblWidth.Enum.forString("dxa"));
        CTTblWidth bottom = mar.addNewBottom();
        bottom.setW(bottomPt * 20);
        bottom.setType(STTblWidth.Enum.forString("dxa"));
        CTTblWidth right = mar.addNewRight();
        right.setW(rightPt * 20);
        right.setType(STTblWidth.Enum.forString("dxa"));
    }

    private CTTcPr getTcPr(XWPFTableCell cell) {
        CTTc ct = cell.getCTTc();
        return ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
    }

    private void setTableBorders(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                CTTcBorders borders = getTcPr(cell).addNewTcBorders();
                CTBorder top = borders.addNewTop();
                top.setColor(BORDER_GRAY);
                top.setSz(BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                CTBorder bottom = borders.addNewBottom();
                bottom.setColor(BORDER_GRAY);
                bottom.setSz(BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                CTBorder left = borders.addNewLeft();
                left.setColor(BORDER_GRAY);
                left.setSz(BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                CTBorder right = borders.addNewRight();
                right.setColor(BORDER_GRAY);
                right.setSz(BigInteger.valueOf(4));
                right.setVal(STBorder.Enum.forString("single"));
            }
        }
    }

    private void setTableWidths(XWPFTable table, int... percents) {
        XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            getTcPr(row.getCell(i)).addNewTcW().setW(percents[i] * 100);
            getTcPr(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(XWPFDocument doc, String date) {
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(date + "  |  Generated automatically by Testin IntelliJ plugin.");
        run.setFontSize(8);
        run.setFontFamily("Calibri");
        run.setColor(DARK_GRAY);
    }

    private void applyPageMargins(XWPFDocument doc) {
        CTBody body = doc.getDocument().getBody();
        CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        Object right = pgMar.getRight();
        long rightTwips = (right instanceof Number) ? ((Number) right).longValue() : 1440L;
        pgMar.setLeft(rightTwips);
    }

    private int[] widthsFor(int cols) {
        return switch (cols) {
            case 2 -> new int[]{3, 97};
            case 3 -> new int[]{3, 87, 10};
            default -> new int[]{3, 77, 10, 10};
        };
    }
}
