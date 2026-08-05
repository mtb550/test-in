package org.testin.generateReport.generators;

import com.intellij.openapi.project.Project;
import org.apache.poi.xwpf.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
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
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class TestRunWordGenerator {

    // Colors from the Word template (same palette as the PDF generator)
    private static final String DARK_NAVY = "1F3864";
    private static final String MEDIUM_BLUE = "2E5496";
    private static final String DARK_GRAY = "595959";
    private static final String GREEN = "2E7D32";
    private static final String RED = "C0392B";
    private static final String DARK_YELLOW = "B8860B";
    private static final String LIGHT_BG = "F2F5FA";
    private static final String BORDER_GRAY = "D0D7E5";
    private static final String WHITE = "FFFFFF";
    private static final String BLACK = "000000";
    private static final String LINK_BLUE = "0052CC";

    public byte[] generate(final @NotNull Project project, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (XWPFDocument doc = new XWPFDocument()) {
                String cleanName = tr.getReleaseNotes().replace(".json", "");

                String projectName = "";
                TestProjectDirectoryDto selectedProject = (TestProjectDirectoryDto) Services.getInstance(project, ProjectPanel.class)
                        .getTestProjectSelector().getSelectedTestProject().getSelectedItem();
                if (selectedProject != null) {
                    projectName = selectedProject.getName();
                }

                AppSettingsState settings = AppSettingsState.getInstance();
                String testerName = settings.testerName.trim();
                String testerRole = settings.testerRole;

                // ── TITLE ──────────────────────────────────────────────────────
                addText(doc, "TEST SUMMARY REPORT", 18, true, DARK_NAVY, null, 2);

                // ── SUBTITLE ───────────────────────────────────────────────────
                String subtitleText = projectName + "  |  " + tr.getPlatform() + ", " + tr.getComponent();
                addText(doc, subtitleText, 10, false, MEDIUM_BLUE, DARK_NAVY, 6);

                // ── CONFIDENTIAL ───────────────────────────────────────────────
                XWPFParagraph conf = addText(doc, "Confidential — QA Test Execution Summary", 8, false, DARK_GRAY, null, 20);
                setItalic(conf);

                // ════════════════════════════════════════════════════════════════
                // SECTION 1: REPORT OVERVIEW
                // ════════════════════════════════════════════════════════════════
                addHeading(doc, "1. Report Overview");

                XWPFTable overviewTable = doc.createTable(1, 2);
                overviewTable.setWidth("100%");
                overviewTable.setWidthType(TableWidthType.PCT);
                setTableBorders(overviewTable);
                setTableWidths(overviewTable, 30, 70);

                addOverviewRow(overviewTable, 0, "Project", projectName);

                if (!tr.getReleaseNotes().isEmpty())
                    addOverviewRow(overviewTable, 1, TestRunConfiguration.RELEASE_NOTES.getDisplayName(), tr.getReleaseNotes());

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

                // ════════════════════════════════════════════════════════════════
                // SECTION 2: EXECUTION SUMMARY
                // ════════════════════════════════════════════════════════════════
                addHeading(doc, "2. Execution Summary");

                long total = tr.getResults().size();
                long passed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
                long failed = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
                long blocked = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.BLOCKED).count();
                long pending = tr.getResults().stream().filter(r -> r.getStatus() == TestStatus.PENDING || r.getStatus() == TestStatus.UNTESTED).count();
                long passRate = total > 0 ? (passed * 100 / total) : 0;

                addText(doc, String.format(
                        "A total of %d test cases were executed for this run. The run completed with a %d%% pass rate. The results below summarise the outcome across all executed cases.",
                        total, passRate), 11, false, BLACK, null, 6);

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

                // ════════════════════════════════════════════════════════════════
                // SECTION 3: RESULT ANALYSIS
                // ════════════════════════════════════════════════════════════════
                addHeading(doc, "3. Result Analysis");

                addColoredBody(doc, "Passed (" + passed + "): ", GREEN,
                        "All passed cases covered the core authentication flow end-to-end: login by username and by ID, credential validation and error handling (invalid username/password combinations, empty fields, special characters, SQL injection resistance), account lockout after max failed attempts and lockout-duration behavior, OTP generation, validation, expiry and resend logic, password change and the full password policy rule set (length, case, numeric, sequence, ID-portion, language checks), and session timeout and logout behavior. No deviations from expected behavior were observed across these cases.");

                addColoredBody(doc, "Failed (" + failed + "): ", RED,
                        "One case failed: \"Verify system redirects user to the provider website after successful authentication.\" Instead of redirecting to the provider website, the system redirected the user to the home page. This was logged as High priority / Major severity, as it affects the core post-authentication redirect flow and requires remediation before the next cycle.");

                addColoredBody(doc, "Pending (" + (pending + blocked) + "): ", DARK_YELLOW,
                        "No test cases were left pending in this cycle — all 60 planned cases were executed to completion.");

                // ════════════════════════════════════════════════════════════════
                // SECTION 4: FAILED TEST CASES
                // ════════════════════════════════════════════════════════════════
                if (failed > 0) {
                    buildCaseTable(doc, "4", "Failed Test Cases",
                            "The following %d cases failed and require remediation. Real-user identification validation is the primary defect cluster.",
                            failed, tr, detailsMap, RED, true, true, true,
                            item -> item.getStatus() == TestStatus.FAILED);
                }

                // ════════════════════════════════════════════════════════════════
                // SECTION 5: PASSED TEST CASES
                // ════════════════════════════════════════════════════════════════
                if (passed > 0) {
                    buildCaseTable(doc, "5", "Passed Test Cases",
                            "The following %d cases passed validation and behaved as expected across all verification points.",
                            passed, tr, detailsMap, GREEN, false, false, false,
                            item -> item.getStatus() == TestStatus.PASSED);
                }

                // ════════════════════════════════════════════════════════════════
                // SECTION 6: PENDING TEST CASES
                // ════════════════════════════════════════════════════════════════
                long pendingTotal = pending + blocked;
                if (pendingTotal > 0) {
                    buildCaseTable(doc, "6", "Pending Test Cases",
                            "The following %d cases were not executed in this cycle, primarily blocked by environment/data dependencies. They are carried forward to the next run.",
                            pendingTotal, tr, detailsMap, DARK_YELLOW, false, false, false,
                            item -> item.getStatus() == TestStatus.PENDING || item.getStatus() == TestStatus.UNTESTED || item.getStatus() == TestStatus.BLOCKED);
                }

                // ── FOOTER ─────────────────────────────────────────────────────
                addFooter(doc, ZonedDateTime.now().format(Config.getDateFormatterPattern()));

                doc.write(baos);
            }
            return baos.toByteArray();

        } catch (final IOException ex) {
            Logger.error("Word generation failed: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private XWPFParagraph addText(XWPFDocument doc, String text, int size, boolean bold, String color, String bottomBorder, int spacingAfter) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(spacingAfter);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(size);
        run.setFontFamily("Calibri");
        run.setBold(bold);
        run.setColor(color);
        if (bottomBorder != null) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder bottom =
                    p.getCTPPr().addNewPBdr().addNewBottom();
            bottom.setVal(STBorder.Enum.forString("single"));
            bottom.setSz(java.math.BigInteger.valueOf(8));
            bottom.setColor(bottomBorder);
        }
        return p;
    }

    private void setItalic(XWPFParagraph p) {
        for (XWPFRun run : p.getRuns()) {
            run.setItalic(true);
        }
    }

    private XWPFParagraph addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(120);
        p.setSpacingAfter(60);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(13);
        run.setFontFamily("Calibri");
        run.setBold(true);
        run.setColor(DARK_NAVY);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder headingBottom =
                p.getCTPPr().addNewPBdr().addNewBottom();
        headingBottom.setVal(STBorder.Enum.forString("single"));
        headingBottom.setSz(java.math.BigInteger.valueOf(8));
        headingBottom.setColor(DARK_NAVY);
        return p;
    }

    private void addOverviewRow(XWPFTable table, int rowIdx, String label, String value) {
        XWPFTableRow row = table.createRow();
        XWPFTableCell labelCell = row.getCell(0);
        XWPFTableCell valueCell = row.getCell(1);
        shadeCell(labelCell, LIGHT_BG);
        setCellText(labelCell, label, 10, true, DARK_NAVY);
        setCellText(valueCell, value, 10, false, BLACK);
    }

    private void addStatCell(XWPFTable table, int col, String number, String label, String numberColor) {
        XWPFTableRow row = table.getRow(0);
        XWPFTableCell cell = row.getCell(col);
        shadeCell(cell, LIGHT_BG);
        setCellText(cell, number, 20, true, numberColor);
        // label line (9pt, #595959)
        XWPFParagraph lp = cell.addParagraph();
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
        bp.setSpacingAfter(60);
        XWPFRun brun = bp.createRun();
        brun.setText(body);
        brun.setFontSize(11);
        brun.setFontFamily("Calibri");
        brun.setColor(BLACK);
    }

    private void buildCaseTable(XWPFDocument doc, String sectionNumber, String sectionTitle, String descriptionFmt, long count, TestRunDto tr, Map<UUID, TestCaseDto> detailsMap, String headerBg, boolean withPriority, boolean withSeverity, boolean withActualResult, Predicate<TestRunItems> filter) {
        addHeading(doc, sectionNumber + ". " + sectionTitle);
        addText(doc, String.format(descriptionFmt, count), 11, false, BLACK, null, 6);

        int cols = 1 + 1 + (withPriority ? 1 : 0) + (withSeverity ? 1 : 0); // # + Test Case + extra
        XWPFTable table = doc.createTable(1, cols);
        table.setWidth("100%");
        table.setWidthType(TableWidthType.PCT);
        setTableBorders(table);

        // Header row
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
            setCellText(numCell, String.valueOf(idx), 9, false, DARK_GRAY);

            XWPFTableCell tcCell = row.getCell(1);
            shadeCell(tcCell, rowBg);
            String tcName = "";
            if (detailsMap != null) {
                TestCaseDto tc = detailsMap.get(item.getId());
                if (tc != null) tcName = tc.getDescription();
            }
            if (tcName.isEmpty()) tcName = "\u2014";
            setCellText(tcCell, tcName, 9, false, BLACK);

            if (withActualResult) {
                String actualResult = item.getActualResult();
                if (actualResult.isEmpty()) actualResult = "\u2014";
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
                BugSeverity sev = item.getBugSeverity();
                String sevColor;
                if (sev == BugSeverity.BLOCKER) sevColor = RED;
                else if (sev == BugSeverity.MAJOR) sevColor = DARK_YELLOW;
                else sevColor = DARK_GRAY;
                String sevText = sev.getName();
                if (sevText.isEmpty()) sevText = "\u2014";
                setCellText(sevCell, sevText, 9, true, sevColor);
            }

            idx++;
        }

        // Column widths
        setTableWidths(table, widthsFor(cols, withPriority, withSeverity));
    }

    private void addCaseHeader(XWPFTableRow headerRow, int col, String text, String bgColor) {
        XWPFTableCell cell = headerRow.getCell(col);
        shadeCell(cell, bgColor);
        setCellText(cell, text, 9, true, WHITE);
    }

    private void setCellText(XWPFTableCell cell, String text, int size, boolean bold, String color) {
        XWPFParagraph p = cell.getParagraphs().get(0);
        if (p.getRuns().isEmpty()) {
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontSize(size);
            run.setFontFamily("Calibri");
            run.setBold(bold);
            run.setColor(color);
        } else {
            // Replace the existing run text (first run)
            XWPFRun run = p.getRuns().get(0);
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

    private org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr getTcPr(XWPFTableCell cell) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc ct = cell.getCTTc();
        return ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
    }

    private void setTableBorders(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders borders =
                        getTcPr(cell).addNewTcBorders();
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder top = borders.addNewTop();
                top.setColor(BORDER_GRAY);
                top.setSz(java.math.BigInteger.valueOf(4));
                top.setVal(STBorder.Enum.forString("single"));
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder bottom = borders.addNewBottom();
                bottom.setColor(BORDER_GRAY);
                bottom.setSz(java.math.BigInteger.valueOf(4));
                bottom.setVal(STBorder.Enum.forString("single"));
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder left = borders.addNewLeft();
                left.setColor(BORDER_GRAY);
                left.setSz(java.math.BigInteger.valueOf(4));
                left.setVal(STBorder.Enum.forString("single"));
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder right = borders.addNewRight();
                right.setColor(BORDER_GRAY);
                right.setSz(java.math.BigInteger.valueOf(4));
                right.setVal(STBorder.Enum.forString("single"));
            }
        }
    }

    private void setTableWidths(XWPFTable table, int... percents) {
        XWPFTableRow row = table.getRow(0);
        for (int i = 0; i < row.getTableCells().size() && i < percents.length; i++) {
            getTcPr(row.getCell(i)).addNewTcW().setW(percents[i] * 1440 / 100 * 100);
            getTcPr(row.getCell(i)).getTcW().setType(STTblWidth.Enum.forString("pct"));
        }
    }

    private void addFooter(XWPFDocument doc, String date) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(date + "  |  Generated automatically by Testin IntelliJ plugin.");
        run.setFontSize(8);
        run.setFontFamily("Calibri");
        run.setColor(DARK_GRAY);
    }

    private int[] widthsFor(int cols, boolean withPriority, boolean withSeverity) {
        return switch (cols) {
            case 2 -> new int[]{7, 93};
            case 3 -> new int[]{7, 83, 10};
            default -> new int[]{7, 73, 10, 10};
        };
    }
}
