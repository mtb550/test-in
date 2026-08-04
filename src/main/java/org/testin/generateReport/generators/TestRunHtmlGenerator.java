package org.testin.generateReport.generators;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.TestStatus;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.settings.AppSettingsState;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestRunHtmlGenerator {

    final String DARK_BLUE = "#1f3864";
    final String MEDIUM_BLUE = "#2e5496";
    final String GREEN = "#2e7d32";
    final String RED = "#c0392b";
    final String ORANGE = "#e46c0a";
    final String GOLD = "#b8860b";
    final String GRAY = "#595959";
    final String LIGHT_BG = "#f2f5fa";
    final String BORDER_COLOR = "#d0d7e5";

    public String generate(final @NotNull Project project, final @NotNull TestRunDirectoryDto trdir,
                           final @NotNull TestRunDto tr, final Map<UUID, TestCaseDto> detailsMap) {
        // Compute summary stats
        final List<TestRunItems> results = tr.getResults();
        final int total = results.size();
        final long passed = results.stream().filter(r -> r.getStatus() == TestStatus.PASSED).count();
        final long failed = results.stream().filter(r -> r.getStatus() == TestStatus.FAILED).count();
        final long blocked = results.stream().filter(r -> r.getStatus() == TestStatus.BLOCKED).count();
        final long pending = results.stream().filter(r -> r.getStatus() == TestStatus.PENDING).count();
        final int passRate = total > 0 ? (int) (passed * 100 / total) : 0;

        // Run-level metadata
        final String runName = tr.getDescription().replace(".json", "");
        final String platform = tr.getPlatform();
        final AppSettingsState settings = AppSettingsState.getInstance();
        final String testerName = settings.testerName;
        final String testerRole = settings.testerRole;
        final String executedBy = testerName; // todo, to be changed to be like pdf generator calss, get all executed by values.
        final String execDate = trdir.getMarker().getCreatedAt().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US));
        final String runStatus = trdir.getMarker().getStatus().getLabel();

        // Project name
        final String projectName = project.getName();

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><style>")
                .append("* { margin: 0; padding: 0; box-sizing: border-box; }")
                .append("body { font-family: Calibri, Arial, sans-serif; color: #000; background: #fff; padding: 40px; }")
                // Title
                .append(".report-title { font-size: 26px; font-weight: bold; color: ").append(DARK_BLUE).append("; }")
                .append(".report-subtitle { font-size: 16px; color: ").append(MEDIUM_BLUE).append("; margin-top: 4px; }")
                .append(".report-conf { font-size: 11px; color: ").append(GRAY).append("; font-style: italic; margin-top: 2px; margin-bottom: 20px; }")
                // Section heading
                .append(".section-title { font-size: 18px; font-weight: bold; color: ").append(DARK_BLUE).append("; margin-top: 28px; margin-bottom: 10px; }")
                .append(".section-title-bar { border-bottom: 2px solid ").append(MEDIUM_BLUE).append("; margin-bottom: 14px; }")
                // Overview table
                .append(".overview-table { border-collapse: collapse; width: 100%; max-width: 700px; }")
                .append(".overview-table td { padding: 6px 12px; border: 1px solid ").append(BORDER_COLOR).append("; font-size: 13px; }")
                .append(".overview-table td.label { background: ").append(LIGHT_BG).append("; font-weight: bold; color: ").append(DARK_BLUE).append("; width: 160px; }")
                .append(".overview-table td.value { color: #000; }")
                // Summary cards
                .append(".summary-text { font-size: 14px; color: #000; margin-bottom: 18px; line-height: 1.5; }")
                .append(".summary-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 28px; }")
                .append(".summary-card { flex: 1; min-width: 120px; text-align: center; background: ").append(LIGHT_BG).append("; border: 1px solid ").append(BORDER_COLOR).append("; border-radius: 6px; padding: 14px 8px; }")
                .append(".summary-card .card-value { font-size: 28px; font-weight: bold; }")
                .append(".summary-card .card-label { font-size: 12px; color: ").append(GRAY).append("; margin-top: 4px; }")
                // Detail table (failed / pending)
                .append(".detail-table { border-collapse: collapse; width: 100%; margin-top: 8px; }")
                .append(".detail-table th { background: ").append(LIGHT_BG).append("; text-align: left; padding: 8px 12px; border: 1px solid ").append(BORDER_COLOR).append("; font-weight: bold; color: ").append(DARK_BLUE).append("; font-size: 13px; }")
                .append(".detail-table td { padding: 8px 12px; border: 1px solid ").append(BORDER_COLOR).append("; font-size: 13px; vertical-align: top; }")
                .append(".detail-table td.seq { text-align: center; width: 40px; color: ").append(GRAY).append("; }")
                // Footer
                .append(".footer { margin-top: 30px; text-align: center; font-size: 11px; color: #888; border-top: 1px solid ").append(BORDER_COLOR).append("; padding-top: 14px; }")
                .append(".footer a { color: #0052cc; text-decoration: none; }")
                .append("</style></head><body>");

        // ═══════════════════════════════════════════════════
        // HEADER
        // ═══════════════════════════════════════════════════
        html.append("<div class='report-title'>TEST SUMMARY REPORT</div>")
                .append("<div class='report-subtitle'>").append(projectName).append("  |  ").append(runName).append("</div>")
                .append("<div class='report-conf'>Confidential — QA Test Execution Summary</div>");

        // ═══════════════════════════════════════════════════
        // SECTION 1: Report Overview
        // ═══════════════════════════════════════════════════
        html.append("<div class='section-title-bar'><div class='section-title'>1. Report Overview</div></div>");

        html.append("<table class='overview-table'>");
        overviewRow(html, "Project", projectName);
        overviewRow(html, "Sprint / Cycle", runName);
        overviewRow(html, "Test Type", "API Functional Testing");
        overviewRow(html, "Platform", platform);
        overviewRow(html, "Executed By", executedBy);
        overviewRow(html, "Execution Date", execDate);
        overviewRow(html, "Run Status", runStatus);
        html.append("</table>");

        // ═══════════════════════════════════════════════════
        // SECTION 2: Execution Summary
        // ═══════════════════════════════════════════════════
        html.append("<div class='section-title-bar'><div class='section-title'>2. Execution Summary</div></div>");

        html.append("<div class='summary-text'>")
                .append("A total of <b>").append(total).append("</b> API functional test cases were executed for ")
                .append("<b>").append(runName).append("</b>. The run completed with a <b>").append(passRate).append("%</b> pass rate. ")
                .append("The results below summarises the outcome across all executed cases.")
                .append("</div>");

        // Summary cards
        html.append("<div class='summary-cards'>");
        summaryCard(html, String.valueOf(total), "Total Cases", DARK_BLUE);
        summaryCard(html, String.valueOf(passed), "Passed", GREEN);
        summaryCard(html, String.valueOf(failed), "Failed", RED);
        summaryCard(html, String.valueOf(blocked), "Blocked", ORANGE);
        summaryCard(html, String.valueOf(pending), "Pending", GOLD);
        summaryCard(html, passRate + "%", "Pass Rate", DARK_BLUE);
        html.append("</div>");

        // ═══════════════════════════════════════════════════
        // SECTION 3: Failed Test Cases
        // ═══════════════════════════════════════════════════
        if (failed > 0) {
            html.append("<div class='section-title-bar'><div class='section-title'>3. Failed Test Cases</div></div>");
            html.append("<div class='summary-text'>The following <b>").append(failed).append("</b> cases failed and require remediation.</div>");
            html.append("<table class='detail-table'>")
                    .append("<tr><th>#</th><th>Test Case</th><th>Priority</th></tr>");

            final AtomicInteger seq = new AtomicInteger(1);
            results.stream()
                    .filter(r -> r.getStatus() == TestStatus.FAILED)
                    .forEach(item -> {
                        TestCaseDto d = detailsMap.get(item.getId());
                        String desc = d != null ? d.getDescription() : "";
                        String bugPriority = item.getBugPriority().name();
                        html.append("<tr>")
                                .append("<td class='seq'>").append(seq.getAndIncrement()).append("</td>")
                                .append("<td>").append(escapedHtml(desc)).append("</td>")
                                .append("<td>").append(bugPriority).append("</td>")
                                .append("</tr>");
                    });
            html.append("</table>");
        }

        // ═══════════════════════════════════════════════════
        // SECTION 4: Pending Test Cases
        // ═══════════════════════════════════════════════════
        if (pending > 0) {
            int sectionNum = failed > 0 ? 4 : 3;
            html.append("<div class='section-title-bar'><div class='section-title'>").append(sectionNum).append(". Pending Test Cases</div></div>");
            html.append("<div class='summary-text'>The following <b>").append(pending).append("</b> cases are pending execution.</div>");
            html.append("<table class='detail-table'>")
                    .append("<tr><th>#</th><th>Test Case</th><th>Priority</th></tr>");

            final AtomicInteger seq = new AtomicInteger(1);
            results.stream()
                    .filter(r -> r.getStatus() == TestStatus.PENDING)
                    .forEach(item -> {
                        TestCaseDto d = detailsMap.get(item.getId());
                        String desc = d != null ? d.getDescription() : "";
                        String bugPriority = item.getBugPriority().name();
                        html.append("<tr>")
                                .append("<td class='seq'>").append(seq.getAndIncrement()).append("</td>")
                                .append("<td>").append(escapedHtml(desc)).append("</td>")
                                .append("<td>").append(bugPriority).append("</td>")
                                .append("</tr>");
                    });
            html.append("</table>");
        }

        // ═══════════════════════════════════════════════════
        // FOOTER
        // ═══════════════════════════════════════════════════
        html.append("<div class='footer'>")
                .append("<p>Prepared by <b>").append(escapedHtml(executedBy)).append("</b> — ")
                .append(escapedHtml(testerRole))
                .append("  |  ")
                .append(execDate.isEmpty() ? "" : execDate)
                .append("</p>")
                .append("<p>Generated automatically by <a href='https://plugins.jetbrains.com/plugin/31514-testin' target='_blank'><strong>Testin</strong></a> IntelliJ plugin.</p>")
                .append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    private void overviewRow(final StringBuilder html, final String label, final String value) {
        html.append("<tr>")
                .append("<td class='label'>").append(label).append("</td>")
                .append("<td class='value'>").append(escapedHtml(value)).append("</td>")
                .append("</tr>");
    }

    private void summaryCard(final StringBuilder html, final String value, final String label, final String color) {
        html.append("<div class='summary-card'>")
                .append("<div class='card-value' style='color: ").append(color).append(";'>").append(value).append("</div>")
                .append("<div class='card-label'>").append(label).append("</div>")
                .append("</div>");
    }

    private String escapedHtml(final String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
