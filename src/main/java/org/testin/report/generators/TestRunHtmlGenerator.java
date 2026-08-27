package org.testin.report.generators;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.markers.DetailRow;
import org.testin.model.TestRunSummary;
import org.testin.model.TestStatus;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import com.intellij.openapi.util.text.StringUtil;
import org.testin.model.BugPriority;
import org.testin.model.BugSeverity;
import org.testin.model.ResultAnalysis;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
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
    final String GREEN = "#748F74";
    final String RED = "#9C4B4F";
    final String ORANGE = "#BD7740";
    final String GRAY = "#595959";
    final String LIGHT_BG = "#f2f5fa";
    final String BORDER_COLOR = "#d0d7e5";


    public @NotNull String generate(final @NotNull Project p, final @NotNull TestRunDirectoryDto trDir, final @NotNull TestRunDto tr, final @NotNull Map<UUID, TestCaseDto> detailsMap) {

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

        // Run-level metadata. The name is the run's own, taken from the node -
        // it used to be the change log with ".json" stripped off it, so a report
        // was titled with a list of stories, and once the change log became
        // several lines it would have been titled with all of them.
        final @NotNull String runName = trDir.getName();

        // The project testin.yml names, which is what the tree shows and what
        // the other three generators already print. This one printed the IDE
        // project's name instead - the automation repository's folder, so a
        // report from nafath-test-case was headed nafath-test-case.
        final @NotNull String projectName = Services.getInstance(p, BoundTestProject.class).name();

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append(styles())
                .append("</head><body>");

        html.append("<button class='theme-toggle' type='button' onclick='testinToggleTheme()'>Dark mode</button>");

        // HEADER
        html.append("<div class='report-title'>TEST SUMMARY REPORT</div>")
                .append("<div class='report-subtitle'>")
                .append(escapedHtml(ReportText.joined("  |  ", projectName, ReportText.joined(", ", tr.getPlatform(), tr.getComponent())))).append("</div>")
                .append("<div class='report-runname'>").append(escapedHtml(runName)).append("</div>")
                .append("<div class='report-conf'>Confidential — QA Test Execution Summary</div>");

        // SECTION 1: Report Overview
        html.append("<div class='section-title-bar'><div class='section-title'>1. Report Overview</div></div>");
        html.append("<table class='overview-table'>");
        for (final DetailRow row : ReportOverview.rowsFor(projectName, trDir, tr, summary)) {
            overviewRow(html, row.caption(), row.value());
        }
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
        summaryCard(html, String.valueOf(total), "Total Cases", "var(--heading)");
        summaryCard(html, String.valueOf(passed), TestStatus.PASSED.getLabel(), "var(--verdict-passed)");
        summaryCard(html, String.valueOf(failed), TestStatus.FAILED.getLabel(), "var(--verdict-failed)");
        summaryCard(html, String.valueOf(blocked), TestStatus.BLOCKED.getLabel(), "var(--verdict-blocked)");
        summaryCard(html, String.valueOf(untested), TestStatus.UNTESTED.getLabel(), "var(--verdict-untested)");
        if (summary.hasRemoved()) summaryCard(html, String.valueOf(summary.removed()), TestStatus.REMOVED.getLabel(), "var(--verdict-removed)");
        summaryCard(html, passRate + "%", "Pass Rate", "var(--heading)");
        html.append("</div>");

        // RESULT ANALYSIS - what the tester wrote about each verdict. This
        // report had no such section at all while the PDF and the Word file both
        // printed one, so the same run read differently depending on the format
        // it was sent in.
        final boolean analysed = ResultAnalysis.anyWrittenIn(tr.getResultAnalysis());

        if (analysed) {
            html.append("<div class='section-title-bar'><div class='section-title'>3. Result Analysis</div></div>");

            for (final ResultAnalysis section : ResultAnalysis.values()) {
                final @NotNull String written = section.writtenIn(tr.getResultAnalysis());
                if (written.isEmpty()) continue;

                // The token named after the verdict, so the heading follows the
                // skin instead of staying a color mixed for white paper - grey
                // text on the dark ground would be all but invisible.
                html.append("<div class='analysis-heading' style='color: var(--verdict-")
                        .append(section.name().toLowerCase(java.util.Locale.ROOT)).append(")'>")
                        .append(section.heading(summary)).append("</div>")
                        .append("<div class='analysis-text'>")
                        // Newlines are left as they are: the class sets
                        // white-space: pre-wrap, so the paragraph breaks where
                        // the tester broke it without any markup being invented.
                        .append(StringUtil.escapeXmlEntities(written))
                        .append("</div>");
            }
        }

        // SECTIONS 3+: one case table per status, empty ones omitted, numbered as
        // printed. Driven by the shared sections, so this report lists the same
        // cases under the same headings as the PDF and the Word version of the
        // same run - including the passed table, which HTML used to leave out.
        int sectionNumber = analysed ? 4 : 3;
        for (final ReportSection section : ReportSection.values()) {
            final long count = section.count(summary);
            if (count == 0) continue;

            appendCaseTable(html, sectionNumber++, section.getTitle(),
                    section.description("<b>" + count + "</b>"),
                    section.name().toLowerCase(java.util.Locale.ROOT), section.isWithFailureDetail(),
                    results, detailsMap, section::matches);
        }

        // FOOTER
        // One centered line, which is what the PDF and the Word file print. This
        // one used to add a "Prepared by" line above it that neither of the other
        // two had, and it named whoever recorded the verdicts rather than whoever
        // generated the report.
        html.append("<div class='footer'>")
                .append(Display.formatDate(ZonedDateTime.now()))
                .append("  |  Generated automatically by ")
                .append("<a href='").append(ReportText.PLUGIN_URL).append("' target='_blank'>Testin</a>")
                .append(" IntelliJ plugin.")
                .append("</div>");

        html.append(themeScript());
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * One section's cases.
     * <p>
     * Priority and severity belong to the failures and nowhere else, which is
     * what {@code withFailureDetail} says and what the PDF has always done. This
     * report printed a Priority column on every table instead - so a passed case
     * was listed under a heading it could not answer, and the cell read EMPTY -
     * and it printed no Severity column at all.
     */
    private void appendCaseTable(final @NotNull StringBuilder html, final int sectionNumber, final @NotNull String title, final @NotNull String blurb, final @NotNull String section, final boolean withFailureDetail, final @NotNull List<TestRunItems> results, final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull Predicate<TestRunItems> filter) {

        html.append("<div class='section-title-bar'><div class='section-title'>").append(sectionNumber).append(". ").append(title).append("</div></div>");
        html.append("<div class='summary-text'>").append(blurb).append("</div>");
        html.append("<table class='detail-table'>")
                .append("<tr style='background: var(--section-").append(section).append(")")
                .append("; color: var(--section-").append(section).append("-ink)'>")
                .append("<th class='seq'>#</th><th>Test Case</th>");

        if (withFailureDetail) html.append("<th class='verdict'>Priority</th><th class='verdict'>Severity</th>");

        html.append("</tr>");

        final @NotNull AtomicInteger seq = new AtomicInteger(1);
        results.stream()
                .filter(filter)
                .forEach(item -> {
                    final @NotNull String desc = ReportedCase.of(detailsMap, item.getId()).getDescription();

                    html.append("<tr>")
                            .append("<td class='seq'>").append(seq.getAndIncrement()).append("</td>")
                            .append("<td>").append(escapedHtml(desc.isEmpty() ? "—" : desc));

                    if (withFailureDetail) {
                        // Under the case it belongs to rather than in a column of
                        // its own: an actual result is a sentence, and a column
                        // wide enough for one would leave the rest of the table
                        // in a strip. The PDF puts it in the same cell.
                        final @NotNull String actual = item.getActualResult();
                        html.append("<div class='actual'>Actual result: ")
                                .append(escapedHtml(actual.isEmpty() ? "—" : actual))
                                .append("</div>");

                        // No em dash for an absent one, unlike the actual
                        // result above it. A missing sentence is worth saying;
                        // a missing stacktrace is not, and a row of dashes down
                        // a failure table is noise a reader learns to skip.
                        final @NotNull String stacktrace = item.getStacktrace();
                        if (!stacktrace.isBlank()) {
                            html.append("<div class='stacktrace'>").append(escapedHtml(stacktrace)).append("</div>");
                        }
                    }

                    html.append("</td>");

                    if (withFailureDetail) {
                        final @NotNull BugPriority priority = item.getBugPriority();
                        final @NotNull BugSeverity severity = item.getBugSeverity();
                        final @NotNull String severityText = severity.getName();

                        html.append("<td class='verdict' style='color: ")
                                .append(priority.getEmphasis().getCssToken()).append("'>")
                                .append(escapedHtml(priority.getName())).append("</td>")
                                .append("<td class='verdict' style='color: ")
                                .append(severity.getEmphasis().getCssToken()).append("'>")
                                .append(escapedHtml(severityText.isEmpty() ? "—" : severityText)).append("</td>");
                    }

                    html.append("</tr>");
                });
        html.append("</table>");
    }

    /**
     * The whole stylesheet.
     * <p>
     * Its own method because it is most of the page by volume and none of it by
     * meaning: what the report actually says was buried under a hundred lines of
     * declarations, and a reader looking for the run's data had to scroll past
     * all of them to reach the first thing that is printed.
     * <p>
     * Built with a builder rather than by adding strings together, which is what
     * the inspection would rather see. It is the same shape the rest of this
     * class uses to put a page together, and a hundred declarations joined by
     * plus signs would read as one expression instead of a list of rules.
     */
    private @NotNull String styles() {
        return new StringBuilder("<style>")

                // Every color is a token, so the report has two skins over one
                // stylesheet. Dark is the ground it opens on; white is a choice
                // the reader makes, and the white values are exactly what this
                // report has always printed.
                .append("/* The skin on screen unless the reader asks for white. */")
                .append(":root {").append(darkTokens()).append(sectionTokens()).append("}")
                .append("/* The white skin: used only when the reader presses the button. */")
                .append(":root[data-theme='light'] {").append(lightTokens()).append("}")

                .append("* { margin: 0; padding: 0; box-sizing: border-box; }")
                .append("body { font-family: Calibri, Arial, sans-serif; color: var(--ink); background: var(--page); padding: 40px; }")

                // Theme switch
                .append(".theme-toggle { position: fixed; top: 16px; right: 16px; font: inherit; font-size: ").append(ReportFont.SMALL.css()).append("; ")
                .append("padding: 6px 12px; border-radius: 6px; cursor: pointer; ")
                .append("background: var(--panel); color: var(--ink); border: 1px solid var(--line); }")
                .append(".theme-toggle:hover { border-color: var(--accent); }")

                // Title
                .append(".report-title { font-size: ").append(ReportFont.TITLE.css()).append("; font-weight: bold; color: var(--heading); }")
                .append(".report-subtitle { font-size: ").append(ReportFont.SUBTITLE.css()).append("; color: var(--accent); margin-top: 4px; }")
                // The rule closes the two names, above the notice - the same
                // weight and color the PDF and the Word file draw it in.
                .append(".report-runname { font-size: ").append(ReportFont.LEAD.css()).append("; color: var(--accent); margin-top: 2px; ")
                .append("padding-bottom: 6px; border-bottom: 2px solid var(--heading); }")
                .append(".analysis-heading { font-size: ").append(ReportFont.LEAD.css()).append("; font-weight: bold; margin-top: 10px; }")
                .append(".analysis-text { font-size: ").append(ReportFont.BODY.css()).append("; color: var(--ink); margin-bottom: 8px; white-space: pre-wrap; }")
                .append(".report-conf { font-size: ").append(ReportFont.CAPTION.css()).append("; color: var(--muted); font-style: italic; margin-top: 6px; margin-bottom: 20px; }")

                // Section heading
                .append(".section-title { font-size: ").append(ReportFont.SECTION.css()).append("; font-weight: bold; color: var(--heading); margin-top: 28px; margin-bottom: 10px; }")
                // Thin, and in the heading color. The PDF draws every section rule at
                // one point and only the header rule at two, so a bold line means
                // "the header ends here" and nothing else.
                .append(".section-title-bar { border-bottom: 1px solid var(--heading); margin-bottom: 14px; }")

                // Overview table
                .append(".overview-table { border-collapse: collapse; width: 100%; max-width: 700px; }")
                .append(".overview-table td { padding: 6px 12px; border: 1px solid var(--line); font-size: ").append(ReportFont.BODY.css()).append("; }")
                // Never wrapped: the labels are field names, and "Platform, Component"
                // broken across two lines reads as two fields. The width is a floor
                // rather than a cap - nowrap makes the column take what the longest
                // name needs, so a longer one added later still sits on one line.
                .append(".overview-table td.label { background: var(--panel); font-weight: bold; color: var(--heading); ")
                .append("width: 240px; white-space: nowrap; }")
                // The change log is written on several lines and has to stay that
                // way: without this the browser folds them into one, so a run
                // covering three stories read as one long sentence.
                .append(".overview-table td.value { color: var(--ink); white-space: pre-wrap; }")

                // Summary cards
                .append(".summary-text { font-size: ").append(ReportFont.LEAD.css()).append("; color: var(--ink); margin-bottom: 18px; line-height: 1.5; }")
                .append(".summary-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 28px; }")
                .append(".summary-card { flex: 1; min-width: 120px; text-align: center; background: var(--panel); border: 1px solid var(--line); border-radius: 6px; padding: 14px 8px; }")
                .append(".summary-card .card-value { font-size: ").append(ReportFont.FIGURE.css()).append("; font-weight: bold; }")
                .append(".summary-card .card-label { font-size: ").append(ReportFont.SMALL.css()).append("; color: var(--muted); margin-top: 4px; }")

                // Detail table (failed / pending)
                .append(".detail-table { border-collapse: collapse; width: 100%; margin-top: 8px; }")
                // Colored per section and centered, the way the PDF and the Word
                // file head the same tables. The background is set on the row
                // rather than here, because it is the section's color and this
                // stylesheet does not know which section it is drawing. It is the
                // same color in both themes: a saturated fill under white text
                // reads on either ground.
                .append(".detail-table th { text-align: center; padding: 8px 12px; border: 1px solid var(--line); font-weight: bold; font-size: ").append(ReportFont.HEADING.css()).append("; }")
                .append(".detail-table td { padding: 8px 12px; border: 1px solid var(--line); font-size: ").append(ReportFont.BODY.css()).append("; vertical-align: top; }")
                // Banded the way the PDF bands the same table: the first
                // data row tinted, then alternating. The header is the
                // first child of the row group, so the first data row is
                // the even one - and the rule names td, so it never
                // repaints the colored header.
                .append(".detail-table tr:nth-child(even) td { background: var(--panel); }")
                .append(".detail-table tr:nth-child(odd) td { background: var(--page); }")
                .append(".detail-table td.seq { text-align: center; color: var(--muted); }")
                // Shrunk to their content: a width of 1% is a browser's way of asking
                // for the narrowest the column can be, and nowrap sets that floor at
                // the longest word. Whatever is left over goes to the test case
                // column, which is the one worth the space.
                .append(".detail-table td.verdict, .detail-table th.verdict { width: 1%; white-space: nowrap; }")
                .append(".detail-table td.verdict { text-align: center; font-weight: bold; }")
                .append(".detail-table td.seq, .detail-table th.seq { width: 1%; white-space: nowrap; }")
                .append(".actual { font-size: ").append(ReportFont.SMALL.css()).append("; color: var(--muted); margin-top: 3px; white-space: pre-wrap; }")
                .append(".stacktrace { font-family: ui-monospace, Consolas, monospace; font-size: ").append(ReportFont.SMALL.css()).append("; color: var(--muted); margin-top: 6px; white-space: pre-wrap; word-break: break-word; }")

                // Footer
                .append(".footer { margin-top: 30px; text-align: center; font-size: ").append(ReportFont.CAPTION.css()).append("; color: var(--footer-ink); border-top: 1px solid var(--line); padding-top: 14px; }")
                .append(".footer a { color: var(--link); text-decoration: none; }")

                // Printing is always the white report, whatever is on screen -
                // dark chrome on paper is a waste of a cartridge.
                .append("/* Printing: the white skin again, whichever is on screen. */")
                .append("@media print { :root, :root[data-theme='dark'] {").append(lightTokens()).append("}")
                .append(".theme-toggle { display: none; } }")


                .append("</style>")
                .toString();
    }

    private void overviewRow(final @NotNull StringBuilder html, final @NotNull String label, final @NotNull String value) {
        html.append("<tr>")
                .append("<td class='label'>").append(label).append("</td>")
                .append("<td class='value'>").append(escapedHtml(value)).append("</td>")
                .append("</tr>");
    }

    /**
     * A table header's fill and the text on it, one pair per section.
     * <p>
     * In the stylesheet rather than on each row, so every color this report uses
     * is declared in one block near the top and a reader who wants to change one
     * changes it once. They are not repeated in the white or the print block: a
     * saturated fill reads on either ground, so there is nothing to override and
     * an edit here holds whichever skin is showing.
     */
    private @NotNull String sectionTokens() {
        final @NotNull StringBuilder tokens = new StringBuilder();

        for (final ReportSection section : ReportSection.values()) {
            final @NotNull String name = section.name().toLowerCase(java.util.Locale.ROOT);
            tokens.append("--section-").append(name).append(": #").append(section.getHexColor()).append(";")
                    .append("--section-").append(name).append("-ink: #").append(section.textHex()).append(";");
        }

        return tokens.toString();
    }

    /**
     * The white skin, as token values only - exactly the colors this report has
     * printed since before it had a second skin. Needed in two places: the
     * reader who asks for it, and anyone printing.
     */
    private @NotNull String lightTokens() {
        return "--page: #fff; --ink: #000; --heading: " + DARK_BLUE + "; --accent: " + MEDIUM_BLUE + ";"
                + "--muted: " + GRAY + "; --panel: " + LIGHT_BG + "; --line: " + BORDER_COLOR + ";"
                + "--footer-ink: #888; --link: #0052cc;"
                + "--verdict-passed: " + GREEN + "; --verdict-failed: " + RED + ";"
                + "--verdict-blocked: " + ORANGE + "; --verdict-untested: " + GRAY + ";"
                + "--verdict-removed: " + GRAY + ";";
    }

    /**
     * The dark skin, as token values only.
     * <p>
     * What the report opens on, so it is the bare {@code :root} block rather
     * than something behind a media query.
     */
    private @NotNull String darkTokens() {
        return "--page: #1e1f22; --ink: #dfe1e5; --heading: #8fb4f2; --accent: #6f9ae8;"
                + "--muted: #9aa0a8; --panel: #2b2d30; --line: #3d4045;"
                + "--footer-ink: #8a9099; --link: #7aa7f0;"
                + "--verdict-passed: #6cc47a; --verdict-failed: #e5675a;"
                + "--verdict-blocked: #e8a33d; --verdict-untested: #a3a9b1;"
                + "--verdict-removed: #a3a9b1;";
    }

    /**
     * Remembers which skin this reader chose, and nothing else.
     * <p>
     * Without a stored choice the report is dark, and the button says which
     * skin it would switch to. Storage is wrapped because a
     * report opened straight from disk may not be allowed any - and a reader who
     * cannot store a choice should still be able to make one for this sitting.
     */
    private @NotNull String themeScript() {
        return "<script>(function(){"
                + "var root=document.documentElement,btn=document.querySelector('.theme-toggle');"
                + "function shown(){return root.getAttribute('data-theme')||'dark';}"
                + "function label(){btn.textContent=shown()==='dark'?'Light mode':'Dark mode';}"
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
                .append("<div class='card-value' style='color: ").append(color).append(";'>").append(value).append("</div>")
                .append("<div class='card-label'>").append(label).append("</div>")
                .append("</div>");
    }

    private @NotNull String escapedHtml(final @NotNull String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
