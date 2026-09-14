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

package org.testin.bug;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.model.Stacktrace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The one owner of what a filed bug says (#28): the team's template, filled in.
 * <p>
 * The wording is in {@code bugReport.md} beside this class and nowhere else,
 * with a named placeholder for each part, so changing how a bug reads is one
 * edit to one file. It is not translated, and nothing translated goes into it:
 * the issue is written for the team's repository, not for this tester's screen.
 * <p>
 * Tester text can neither break the template nor act on GitHub. A cell escapes
 * its pipes and keeps its line breaks as {@code <br>}; a heading or a rule typed
 * into a section stays text; code sits in a fence longer than any run of
 * backticks inside it; and an {@code @name} or a {@code #123} mentions nobody
 * and links nothing.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BugTemplate {

    /**
     * What a part says when Testin has nothing to put in it.
     */
    static final @NotNull String NOT_AVAILABLE = "n\\a";

    private static final @NotNull String RESOURCE = "bugReport.md";
    private static final @NotNull String SEPARATOR = " · ";

    /**
     * A zero-width space, as an entity. Between {@code @} and a name, or
     * {@code #} and a number, GitHub sees neither a mention nor a reference,
     * and the reader sees no difference.
     */
    private static final @NotNull String UNLINK = "&#8203;";

    private static final @NotNull Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)}}");
    private static final @NotNull Pattern MENTION = Pattern.compile("(?<!\\w)@(?=[A-Za-z0-9])");
    private static final @NotNull Pattern REFERENCE = Pattern.compile("(?<!&)#(?=\\d)");
    private static final @NotNull Pattern HEADING = Pattern.compile("^( {0,3})#");
    private static final @NotNull Pattern RULE = Pattern.compile("^( {0,3})(?=(?:[-=]+|(?:[-*_] *){3,}) *$)");
    private static final @NotNull Pattern BACKTICKS = Pattern.compile("`+");
    private static final @NotNull Pattern TRAILING_LINE_BREAKS = Pattern.compile("\\R+\\z");

    private static final @NotNull String TEMPLATE = load();

    /**
     * The issue's body, from the facts and the link to the test case's file,
     * which is left out when it could not be built.
     */
    static @NotNull String body(final @NotNull BugFacts facts, final @NotNull Optional<String> testCaseLink) {
        final @NotNull String shortId = facts.testCaseId().toString().substring(0, 8);

        return fill(Map.ofEntries(
                Map.entry("severity", cell(facts.severity().getInBugReport())),
                Map.entry("priority", cell(facts.priority().getInBugReport())),
                Map.entry("platform", cell(facts.platform())),
                Map.entry("notAvailable", NOT_AVAILABLE),
                Map.entry("actualResult", section(facts.actualResult())),
                Map.entry("expectedResult", section(facts.expectedResult())),
                Map.entry("steps", steps(facts.steps())),
                Map.entry("testData", codeBlock(facts.testData())),
                Map.entry("exception", exception(facts.stacktrace())),
                Map.entry("screenshots", screenshots(facts.stacktrace().screenshots().size())),
                Map.entry("testRun", cell(facts.testRun())),
                Map.entry("executed", cell(facts.executed())),
                Map.entry("browserDeviceLanguage", String.join(SEPARATOR, cell(facts.browser()), cell(facts.device()), cell(facts.language()))),
                Map.entry("commit", cell(facts.commit())),
                Map.entry("testCase", testCaseLink.map(link -> "[" + shortId + "](" + link + ")").orElse(shortId)),
                Map.entry("testSet", cell(facts.testSetName())),
                Map.entry("tag", inline("#" + facts.testSetName().replaceAll("\\s+", "")))));
    }

    /**
     * The name a screenshot is attached under, counting from one. The body
     * refers to it by this name, and {@code gh} swaps the reference for the
     * uploaded image.
     */
    static @NotNull String screenshotFile(final int number) {
        return "screenshot-" + number + ".png";
    }

    /**
     * In one pass, so a placeholder a tester typed into a value is written as
     * text and never filled.
     */
    private static @NotNull String fill(final @NotNull Map<String, String> parts) {
        return PLACEHOLDER.matcher(TEMPLATE).replaceAll(placeholder -> Matcher.quoteReplacement(parts.getOrDefault(placeholder.group(1), placeholder.group())));
    }

    /**
     * A table cell: backslashes first, then pipes, and line breaks as
     * {@code <br>}.
     */
    static @NotNull String cell(final @NotNull String value) {
        if (value.isBlank()) return NOT_AVAILABLE;

        return inline(value.strip().replace("\\", "\\\\").replace("|", "\\|")).replaceAll("\\R", "<br>");
    }

    /**
     * Text under a heading, where a line typed as {@code # title} or
     * {@code ---} would otherwise become a heading of its own.
     */
    static @NotNull String section(final @NotNull String value) {
        if (value.isBlank()) return NOT_AVAILABLE;

        return blockText(value.strip());
    }

    /**
     * Numbered one after another, blank steps left out: GitHub numbers a list
     * from its first item whatever the rest say, so a gap would read one way in
     * the dialog and another on the issue. A step's second line is indented to
     * stay inside it.
     */
    static @NotNull String steps(final @NotNull List<String> steps) {
        final @NotNull List<String> written = steps.stream().filter(step -> !step.isBlank()).toList();
        if (written.isEmpty()) return NOT_AVAILABLE;

        return IntStream.range(0, written.size())
                .mapToObj(index -> numbered(index + 1, written.get(index)))
                .collect(Collectors.joining("\n"));
    }

    private static @NotNull String numbered(final int number, final @NotNull String step) {
        final @NotNull String marker = number + ". ";

        return marker + blockText(step.strip()).replace("\n", "\n" + " ".repeat(marker.length()));
    }

    /**
     * Verbatim, because what is in it is used rather than read.
     */
    static @NotNull String codeBlock(final @NotNull String value) {
        if (value.isBlank()) return NOT_AVAILABLE;

        final @NotNull String fence = "`".repeat(Math.max(3, longestBackticks(value) + 1));
        return fence + "\n" + TRAILING_LINE_BREAKS.matcher(value).replaceAll("") + "\n" + fence;
    }

    /**
     * The stacktrace's text, folded under its first line. The screenshots are
     * not in it: they have their own heading.
     */
    static @NotNull String exception(final @NotNull Stacktrace stacktrace) {
        final @NotNull String text = stacktrace.text();
        if (text.isEmpty()) return NOT_AVAILABLE;

        return "<details>\n<summary>" + html(stacktrace.firstLine()) + "</summary>\n\n" + codeBlock(text) + "\n</details>";
    }

    private static @NotNull String screenshots(final int count) {
        if (count == 0) return NOT_AVAILABLE;

        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> "![Screenshot " + number + "](./" + screenshotFile(number) + ")")
                .collect(Collectors.joining("\n"));
    }

    private static @NotNull String blockText(final @NotNull String text) {
        return inline(text).lines().map(BugTemplate::literalLine).collect(Collectors.joining("\n"));
    }

    private static @NotNull String literalLine(final @NotNull String line) {
        final @NotNull Matcher heading = HEADING.matcher(line);
        if (heading.find()) return heading.replaceFirst("$1\\\\#");

        return RULE.matcher(line).replaceFirst("$1\\\\");
    }

    /**
     * Text in the flow of Markdown: no HTML tag opens, and nothing links.
     */
    private static @NotNull String inline(final @NotNull String text) {
        return unlinked(text.replace("<", "&lt;"));
    }

    private static @NotNull String html(final @NotNull String text) {
        return unlinked(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"));
    }

    private static @NotNull String unlinked(final @NotNull String text) {
        return REFERENCE.matcher(MENTION.matcher(text).replaceAll("@" + UNLINK)).replaceAll("#" + UNLINK);
    }

    private static int longestBackticks(final @NotNull String text) {
        return BACKTICKS.matcher(text).results().mapToInt(run -> run.group().length()).max().orElse(0);
    }

    private static @NotNull String load() {
        try (final @Nullable InputStream template = BugTemplate.class.getResourceAsStream(RESOURCE)) {
            if (template != null) return new String(template.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");

            Logger.error("The bug report template is not in the plugin: " + RESOURCE);
        } catch (final IOException ex) {
            Logger.error("The bug report template could not be read: " + ex.getMessage());
        }
        return "";
    }
}
