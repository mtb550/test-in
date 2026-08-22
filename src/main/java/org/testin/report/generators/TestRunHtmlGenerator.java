package org.testin.report.generators;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunSummary;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Display;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class TestRunHtmlGenerator {

    final String DARK_BLUE = "#1f3864";
    final String MEDIUM_BLUE = "#2e5496";
    final String GREEN = "#2e7d32";
    final String RED = "#c0392b";
    final String ORANGE = "#e46c0a";
    final String GRAY = "#595959";
    final String LIGHT_BG = "#f2f5fa";
    final String BORDER_COLOR = "#d0d7e5";

    public @NotNull String generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir,
                                    final @NotNull TestRunDto tr,
                                    final @NotNull Map<UUID, TestCaseDto> detailsMap) {

        // Compute summary stats
        final @NotNull List<TestRunItems> results = tr.getResults();
        final int total = results.size();
        // One traversal counts every status; a new TestStatus constant is
        // included automatically instead of needing another filter pass.
        final @NotNull TestRunSummary summary = TestRunSummary.of(results);
        final long passed = summary.passed();
        final long failed = summary.failed();
        final long blocked = summary.blocked();
        final long untested = summary.untested();
        final int passRate = summary.passRate();

        // Run-level metadata
        final @NotNull String runName = tr.getChangeLog().replace(".json", "");
        final @NotNull String platform = tr.getPlatform();
        final @NotNull AppSettingsState settings = Services.getInstance(p, AppSettingsState.class);
        final @NotNull String testerRole = settings.testerRole;
        final @NotNull String executedBy = summary.executedBy();
        final @NotNull String executionStarted = Display.formatDate(tr.getExecutionStartedAt());
        final @NotNull String executionEnded = Display.formatDate(tr.getExecutionEndedAt());
        final @NotNull String runStatus = trDir.getMarker().getStatus().getLabel();

        // Project name
        final @NotNull String projectName = p.getName();

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

        // HEADER
        html.append("<div class='report-title'>TEST SUMMARY REPORT</div>")
                .append("<div class='report-subtitle'>").append(projectName).append("  |  ").append(runName).append("</div>")
                .append("<div class='report-conf'>Confidential — QA Test Execution Summary</div>");

        // SECTION 1: Report Overview
        html.append("<div class='section-title-bar'><div class='section-title'>1. Report Overview</div></div>");
        html.append("<table class='overview-table'>");
        overviewRow(html, "Project", projectName);
        overviewRow(html, "Sprint / Cycle", runName);
        overviewRow(html, "Test Type", "API Functional Testing");
        overviewRow(html, "Platform", platform);
        overviewRow(html, "Executed By", executedBy);
        overviewRow(html, "Execution Started", executionStarted);
        overviewRow(html, "Execution Ended", executionEnded);
        overviewRow(html, "Run Status", runStatus);
        html.append("</table>");

        // SECTION 2: Execution Summary
        html.append("<div class='section-title-bar'><div class='section-title'>2. Execution Summary</div></div>");

        html.append("<div class='summary-text'>")
                .append("<b>").append(runName).append("</b> holds <b>").append(total).append("</b> test cases, of which <b>")
                .append(summary.executed()).append("</b> were executed. Of those, <b>").append(passRate).append("%</b> passed. ")
                .append("The results below summarize the outcome.")
                .append("</div>");

        // Summary cards
        html.append("<div class='summary-cards'>");
        summaryCard(html, String.valueOf(total), "Total Cases", DARK_BLUE);
        summaryCard(html, String.valueOf(passed), "Passed", GREEN);
        summaryCard(html, String.valueOf(failed), "Failed", RED);
        summaryCard(html, String.valueOf(blocked), "Blocked", ORANGE);
        summaryCard(html, String.valueOf(untested), "Untested", GRAY);
        if (summary.hasRemoved()) summaryCard(html, String.valueOf(summary.removed()), "Removed", GRAY);
        summaryCard(html, passRate + "%", "Pass Rate", DARK_BLUE);
        html.append("</div>");

        // SECTIONS 3+: one case table per status, empty ones omitted, numbered as
        // printed. Driven by the shared sections, so this report lists the same
        // cases under the same headings as the PDF and the Word version of the
        // same run - including the passed table, which HTML used to leave out.
        int sectionNumber = 3;
        for (final ReportSection section : ReportSection.values()) {
            final long count = section.count(summary);
            if (count == 0) continue;

            appendCaseTable(html, sectionNumber++, section.getTitle(),
                    section.description("<b>" + count + "</b>"),
                    results, detailsMap, section::matches);
        }

        // FOOTER
        html.append("<div class='footer'>")
                .append("<p>Prepared by <b>").append(escapedHtml(executedBy)).append("</b> — ")
                .append(escapedHtml(testerRole))
                .append("  |  ")
                // When the report was generated, as the PDF and Word footers say.
                .append(Display.formatDate(ZonedDateTime.now()))
                .append("</p>")
                .append("<p>Generated automatically by <a href='https://plugins.jetbrains.com/plugin/31514-testin' target='_blank'><strong>Testin</strong></a> IntelliJ plugin.</p>")
                .append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * A numbered section listing the cases the filter accepts. Failed and Pending
     * differ only in the number, the heading, the blurb and that filter - the
     * table itself is one shape, so it is written once.
     */
    private void appendCaseTable(final @NotNull StringBuilder html, final int sectionNumber,
                                 final @NotNull String title, final @NotNull String blurb,
                                 final @NotNull List<TestRunItems> results,
                                 final @NotNull Map<UUID, TestCaseDto> detailsMap,
                                 final @NotNull Predicate<TestRunItems> filter) {

        html.append("<div class='section-title-bar'><div class='section-title'>").append(sectionNumber).append(". ").append(title).append("</div></div>");
        html.append("<div class='summary-text'>").append(blurb).append("</div>");
        html.append("<table class='detail-table'>")
                .append("<tr><th>#</th><th>Test Case</th><th>Priority</th></tr>");

        final @NotNull AtomicInteger seq = new AtomicInteger(1);
        results.stream()
                .filter(filter)
                .forEach(item -> {
                    final @NotNull String desc = ReportedCase.of(detailsMap, item.getId()).getDescription();
                    final @NotNull String bugPriority = item.getBugPriority().name();
                    html.append("<tr>")
                            .append("<td class='seq'>").append(seq.getAndIncrement()).append("</td>")
                            .append("<td>").append(escapedHtml(desc)).append("</td>")
                            .append("<td>").append(bugPriority).append("</td>")
                            .append("</tr>");
                });
        html.append("</table>");
    }

    private void overviewRow(final @NotNull StringBuilder html, final @NotNull String label,
                             final @NotNull String value) {
        html.append("<tr>")
                .append("<td class='label'>").append(label).append("</td>")
                .append("<td class='value'>").append(escapedHtml(value)).append("</td>")
                .append("</tr>");
    }

    private void summaryCard(final @NotNull StringBuilder html, final @NotNull String value,
                             final @NotNull String label, final @NotNull String color) {
        html.append("<div class='summary-card'>")
                .append("<div class='card-value' style='color: ").append(color).append(";'>").append(value).append("</div>")
                .append("<div class='card-label'>").append(label).append("</div>")
                .append("</div>");
    }

    private @NotNull String escapedHtml(final @NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
