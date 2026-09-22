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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.BugIssueUrl;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.ResultAnalysis;
import org.testin.model.TestRunConfiguration;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.DetailRow;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.RunEditorAttributes;
import org.testin.util.Bundle;
import org.testin.util.Display;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class TestRunHtmlGenerator {
    final String DARK_BLUE = "#1f3864";
    final String MEDIUM_BLUE = "#2e5496";
    final String GREEN = "#748F74";
    final String RED = "#9C4B4F";
    final String ORANGE = "#BD7740";
    final String GRAY = "#595959";
    final String LIGHT_BG = "#f2f5fa";
    final String BORDER_COLOR = "#d0d7e5";

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public @NotNull String generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr) {
        final @NotNull List<TestRunItems> results = tr.getResults();
        final int total = results.size();
        final @NotNull TestRunSummary summary = TestRunSummary.of(results);
        final int passRate = summary.passRate();

        final @NotNull String runName = trDir.getName();

        final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append(styles())
                .append("</head><body>");

        html.append("<button class='theme-toggle' type='button' onclick='testinToggleTheme()'")
                .append(" data-light='").append(StringUtil.escapeXmlEntities(Bundle.message("report.theme.light"))).append("'")
                .append(" data-dark='").append(StringUtil.escapeXmlEntities(Bundle.message("report.theme.dark"))).append("'>")
                .append(Bundle.message("report.theme.light")).append("</button>");

        html.append("<div class='report-title'>").append(Bundle.message("report.title")).append("</div>")
                .append("<div class='report-subtitle'>")
                .append(StringUtil.escapeXmlEntities(ReportText.joined("  |  ", projectName, ReportText.joined(", ", TestRunConfiguration.PLATFORM.valueIn(trDir.getMarker()), TestRunConfiguration.COMPONENT.valueIn(trDir.getMarker()))))).append("</div>")
                .append("<div class='report-run-name'>").append(StringUtil.escapeXmlEntities(runName)).append("</div>")
                .append("<div class='report-conf'>").append(Bundle.message("report.confidential")).append("</div>");

        html.append("<div class='section-title-bar'><div class='section-title'>").append(Bundle.message("report.heading.overview")).append("</div></div>");
        html.append("<table class='overview-table'>");
        for (final DetailRow row : ReportOverview.rowsFor(projectName, trDir, summary)) {
            overviewRow(html, row.caption(), row.value());
        }
        html.append("</table>");

        html.append("<div class='section-title-bar'><div class='section-title'>").append(Bundle.message("report.heading.execution")).append("</div></div>");

        html.append("<div class='summary-text'>")
                .append(Bundle.message("report.summary.named",
                        "<b>" + StringUtil.escapeXmlEntities(runName) + "</b>",
                        "<b>" + total + "</b>",
                        "<b>" + summary.executed() + "</b>",
                        "<b>" + passRate + "%</b>"))
                .append("</div>");

        html.append("<div class='summary-cards'>");
        for (final ReportTile tile : ReportTile.shownFor(summary)) {
            summaryCard(html, tile.valueIn(summary), tile.getLabel(), tile.getCssToken());
        }
        html.append("</div>");

        final boolean analyzed = ResultAnalysis.anyWrittenIn(trDir.getMarker().getResultAnalysis());

        if (analyzed) {
            html.append("<div class='section-title-bar'><div class='section-title'>").append(Bundle.message("report.heading.analysis")).append("</div></div>");

            for (final ResultAnalysis section : ResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(trDir.getMarker().getResultAnalysis());
                if (written.isEmpty()) continue;

                html.append("<div class='analysis-heading' style='color: var(--verdict-")
                        .append(section.name().toLowerCase(Locale.ROOT)).append(")'>")
                        .append(section.heading(summary)).append("</div>")
                        .append("<div class='analysis-text'>")
                        .append(StringUtil.escapeXmlEntities(written))
                        .append("</div>");
            }
        }

        int sectionNumber = analyzed ? 4 : 3;
        for (final ReportSection section : ReportSection.values()) {
            final long count = section.count(summary);
            if (count == 0) continue;

            appendTestCaseTable(html, sectionNumber++, section.getTitle(),
                    section.description("<b>" + count + "</b>"),
                    section.name().toLowerCase(Locale.ROOT), section.isWithFailureDetail(),
                    results, section::matches);
        }

        html.append("<div class='footer'>")
                .append(Display.formatDate(ZonedDateTime.now()))
                .append(Bundle.message("report.footer.prefix"))
                .append("<a href='").append(ReportText.PLUGIN_URL).append("' target='_blank'>Testin</a>")
                .append(Bundle.message("report.footer.suffix"))
                .append("</div>");

        html.append(themeScript());
        html.append("</body></html>");
        return html.toString();
    }

    // Rule-REPORT-019
    private void appendTestCaseTable(final @NotNull StringBuilder html, final int sectionNumber, final @NotNull String title, final @NotNull String blurb, final @NotNull String section, final boolean withFailureDetail, final @NotNull List<TestRunItems> results, final @NotNull Predicate<TestRunItems> filter) {
        html.append("<div class='section-title-bar'><div class='section-title'>").append(sectionNumber).append(". ").append(title).append("</div></div>");
        html.append("<div class='summary-text'>").append(blurb).append("</div>");
        html.append("<table class='detail-table'>")
                .append("<tr style='background: var(--section-").append(section).append(")")
                .append("; color: var(--section-").append(section).append("-ink)'>")
                .append("<th class='seq'>#</th><th>").append(Bundle.message("caption.test.case")).append("</th>");

        if (withFailureDetail) {
            html.append("<th class='verdict'>").append(RunEditorAttributes.BUG_PRIORITY.getName()).append("</th>")
                    .append("<th class='verdict'>").append(RunEditorAttributes.BUG_SEVERITY.getName()).append("</th>");
        }

        html.append("</tr>");

        final @NotNull AtomicInteger seq = new AtomicInteger(1);
        results.stream()
                .filter(filter)
                .forEach(item -> {
                    final @NotNull String desc = item.shownTestCase().getDescription();

                    html.append("<tr>")
                            .append("<td class='seq'>").append(seq.getAndIncrement()).append("</td>")
                            .append("<td>").append(StringUtil.escapeXmlEntities(desc.isEmpty() ? "—" : desc));

                    if (withFailureDetail) {
                        final @NotNull String actual = item.getActualResult();
                        html.append("<div class='actual'>")
                                .append(Bundle.message("report.actual.result", StringUtil.escapeXmlEntities(actual.isEmpty() ? "—" : actual)));

                        item.bugIssue().ifPresent(url -> html.append(" (<a href='").append(StringUtil.escapeXmlEntities(url)).append("' target='_blank'>")
                                .append(StringUtil.escapeXmlEntities(BugIssueUrl.shortReference(url)))
                                .append("</a>)"));
                        html.append("</div>");

                        final @NotNull String stacktrace = item.getStacktrace();
                        if (!stacktrace.isBlank()) {
                            html.append("<div class='stacktrace'>").append(StringUtil.escapeXmlEntities(stacktrace)).append("</div>");
                        }
                    }

                    html.append("</td>");

                    if (withFailureDetail) {
                        final @NotNull BugPriority priority = item.getBugPriority();
                        final @NotNull BugSeverity severity = item.getBugSeverity();
                        final @NotNull String severityText = severity.getLabel();

                        html.append("<td class='verdict' style='color: ")
                                .append(priority.getEmphasis().getCssToken()).append("'>")
                                .append(StringUtil.escapeXmlEntities(priority.getLabel())).append("</td>")
                                .append("<td class='verdict' style='color: ")
                                .append(severity.getEmphasis().getCssToken()).append("'>")
                                .append(StringUtil.escapeXmlEntities(severityText.isEmpty() ? "—" : severityText)).append("</td>");
                    }

                    html.append("</tr>");
                });
        html.append("</table>");
    }

    private @NotNull String styles() {
        return "<style>"

                + "/* The skin on screen unless the reader asks for white. */"
                + ":root {" + darkTokens() + sectionTokens() + "}"
                + "/* The white skin: used only when the reader presses the button. */"
                + ":root[data-theme='light'] {" + lightTokens() + "}"

                + "* { margin: 0; padding: 0; box-sizing: border-box; }"
                + "body { font-family: Calibri, Arial, sans-serif; color: var(--ink); background: var(--page); padding: 40px; }"

                + ".theme-toggle { position: fixed; top: 16px; right: 16px; font: inherit; font-size: " + ReportFont.SMALL.css() + "; "
                + "padding: 6px 12px; border-radius: 6px; cursor: pointer; "
                + "background: var(--panel); color: var(--ink); border: 1px solid var(--line); }"
                + ".theme-toggle:hover { border-color: var(--accent); }"

                + ".report-title { font-size: " + ReportFont.TITLE.css() + "; font-weight: bold; color: var(--heading); }"
                + ".report-subtitle { font-size: " + ReportFont.SUBTITLE.css() + "; color: var(--accent); margin-top: 4px; }"
                + ".report-run-name { font-size: " + ReportFont.LEAD.css() + "; color: var(--accent); margin-top: 2px; "
                + "padding-bottom: 6px; border-bottom: 2px solid var(--heading); }"
                + ".analysis-heading { font-size: " + ReportFont.LEAD.css() + "; font-weight: bold; margin-top: 10px; }"
                + ".analysis-text { font-size: " + ReportFont.BODY.css() + "; color: var(--ink); margin-bottom: 8px; white-space: pre-wrap; }"
                + ".report-conf { font-size: " + ReportFont.CAPTION.css() + "; color: var(--muted); font-style: italic; margin-top: 6px; margin-bottom: 20px; }"

                + ".section-title { font-size: " + ReportFont.SECTION.css() + "; font-weight: bold; color: var(--heading); margin-top: 28px; margin-bottom: 10px; }"
                + ".section-title-bar { border-bottom: 1px solid var(--heading); margin-bottom: 14px; }"

                + ".overview-table { border-collapse: collapse; width: 100%; max-width: 700px; }"
                + ".overview-table td { padding: 6px 12px; border: 1px solid var(--line); font-size: " + ReportFont.BODY.css() + "; }"
                + ".overview-table td.label { background: var(--panel); font-weight: bold; color: var(--heading); "
                + "width: 240px; white-space: nowrap; }"
                + ".overview-table td.value { color: var(--ink); white-space: pre-wrap; }"

                + ".summary-text { font-size: " + ReportFont.LEAD.css() + "; color: var(--ink); margin-bottom: 18px; line-height: 1.5; }"
                + ".summary-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 28px; }"
                + ".summary-card { flex: 1; min-width: 120px; text-align: center; background: var(--panel); border: 1px solid var(--line); border-radius: 6px; padding: 14px 8px; }"
                + ".summary-card .card-value { font-size: " + ReportFont.FIGURE.css() + "; font-weight: bold; }"
                + ".summary-card .card-label { font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 4px; }"

                + ".detail-table { border-collapse: collapse; width: 100%; margin-top: 8px; }"
                + ".detail-table th { text-align: center; padding: 8px 12px; border: 1px solid var(--line); font-weight: bold; font-size: " + ReportFont.HEADING.css() + "; }"
                + ".detail-table td { padding: 8px 12px; border: 1px solid var(--line); font-size: " + ReportFont.BODY.css() + "; vertical-align: top; }"
                + ".detail-table tr:nth-child(even) td { background: var(--panel); }"
                + ".detail-table tr:nth-child(odd) td { background: var(--page); }"
                + ".detail-table td.seq { text-align: center; color: var(--muted); }"
                + ".detail-table td.verdict, .detail-table th.verdict { width: 1%; white-space: nowrap; }"
                + ".detail-table td.verdict { text-align: center; font-weight: bold; }"
                + ".detail-table td.seq, .detail-table th.seq { width: 1%; white-space: nowrap; }"
                + ".actual { font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 3px; white-space: pre-wrap; }"
                + ".stacktrace { font-family: ui-monospace, Consolas, monospace; font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 6px; white-space: pre-wrap; word-break: break-word; }"

                + ".footer { margin-top: 30px; text-align: center; font-size: " + ReportFont.CAPTION.css() + "; color: var(--footer-ink); border-top: 1px solid var(--line); padding-top: 14px; }"
                + ".footer a { color: var(--link); text-decoration: none; }"

                + "/* Printing: the white skin again, whichever is on screen. */"
                + "@media print { :root, :root[data-theme='dark'] {" + lightTokens() + "}"
                + ".theme-toggle { display: none; } }"

                + "</style>";
    }

    private void overviewRow(final @NotNull StringBuilder html, final @NotNull String label, final @NotNull String value) {
        html.append("<tr>")
                .append("<td class='label'>").append(StringUtil.escapeXmlEntities(label)).append("</td>")
                .append("<td class='value'>").append(StringUtil.escapeXmlEntities(value)).append("</td>")
                .append("</tr>");
    }

    private @NotNull String sectionTokens() {
        final @NotNull StringBuilder tokens = new StringBuilder();

        for (final ReportSection section : ReportSection.values()) {
            final @NotNull String name = section.name().toLowerCase(Locale.ROOT);
            tokens.append("--section-").append(name).append(": #").append(section.getHexColor()).append(";")
                    .append("--section-").append(name).append("-ink: #").append(section.textHex()).append(";");
        }

        return tokens.toString();
    }

    private @NotNull String lightTokens() {
        return "--page: #fff; --ink: #000; --heading: " + DARK_BLUE + "; --accent: " + MEDIUM_BLUE + ";"
                + "--muted: " + GRAY + "; --panel: " + LIGHT_BG + "; --line: " + BORDER_COLOR + ";"
                + "--footer-ink: #888; --link: #0052cc;"
                + "--verdict-passed: " + GREEN + "; --verdict-failed: " + RED + ";"
                + "--verdict-blocked: " + ORANGE + "; --verdict-untested: " + GRAY + ";"
                + "--verdict-removed: " + GRAY + ";";
    }

    private @NotNull String darkTokens() {
        return "--page: #1e1f22; --ink: #dfe1e5; --heading: #8fb4f2; --accent: #6f9ae8;"
                + "--muted: #9aa0a8; --panel: #2b2d30; --line: #3d4045;"
                + "--footer-ink: #8a9099; --link: #7aa7f0;"
                + "--verdict-passed: #6cc47a; --verdict-failed: #e5675a;"
                + "--verdict-blocked: #e8a33d; --verdict-untested: #a3a9b1;"
                + "--verdict-removed: #a3a9b1;";
    }

    private @NotNull String themeScript() {
        return "<script>(function(){"
                + "var root=document.documentElement,btn=document.querySelector('.theme-toggle');"
                + "function shown(){return root.getAttribute('data-theme')||'dark';}"
                + "function label(){btn.textContent=shown()==='dark'?btn.dataset.light:btn.dataset.dark;}"
                + "try{var saved=localStorage.getItem('testin.report.theme');if(saved)root.setAttribute('data-theme',saved);}catch(e){}"
                + "label();"
                + "window.testinToggleTheme=function(){var next=shown()==='dark'?'light':'dark';"
                + "root.setAttribute('data-theme',next);"
                + "try{localStorage.setItem('testin.report.theme',next);}catch(e){}"
                + "label();};"
                + "})();</script>";
    }

    private void summaryCard(final @NotNull StringBuilder html, final @NotNull String value, final @NotNull String label, final @NotNull String color) {
        html.append("<div class='summary-card'>")
                .append("<div class='card-value' style='color: ").append(color).append(";'>")
                .append(StringUtil.escapeXmlEntities(value)).append("</div>")
                .append("<div class='card-label'>").append(StringUtil.escapeXmlEntities(label)).append("</div>")
                .append("</div>");
    }
}
