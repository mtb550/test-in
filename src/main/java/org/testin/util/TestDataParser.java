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

package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.Groups;
import org.testin.model.Priority;
import org.testin.model.TestCaseStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Defensive parsing of values imported from tables and external files.
 * <p>
 * <b>An empty answer means Testin could not read the text</b> - never that the
 * text was empty. Blank is a value here: it clears a date and it clears the
 * groups, because those have an empty form of their own, and it keeps the
 * priority and the status, because those do not. What none of them does is
 * choose something the tester did not type and say nothing (#204, #264).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestDataParser {

    private static final @NotNull Pattern MULTI_STEP_LINE = Pattern.compile(".*\\s\\d+[-.].*");
    private static final @NotNull Pattern STEP_SEPARATOR = Pattern.compile("(\\s)(?=\\d+[-.])");
    private static final @NotNull Pattern STEP_PREFIX = Pattern.compile("^\\d+[-.]\\s*");
    /**
     * The plugin's timestamp without its leading weekday, which is stripped
     * rather than matched - see {@link #date}.
     */
    private static final @NotNull DateTimeFormatter WITHOUT_WEEKDAY =
            DateTimeFormatter.ofPattern("dd-MM-yyyy 'At' HH:mm:ss '['VV']'", Locale.US);

    public static @NotNull List<String> steps(final @NotNull String rawSteps) {
        if (rawSteps.isBlank()) return new ArrayList<>();
        String text = rawSteps;
        if (!text.contains("\n") && MULTI_STEP_LINE.matcher(text).matches()) {
            text = STEP_SEPARATOR.matcher(text).replaceAll("\n");
        }
        return Arrays.stream(text.split("\n"))
                .map(line -> STEP_PREFIX.matcher(line).replaceFirst("").trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * UC-SHARE-005, Rule-SHARE-110.
     * <p>
     * Whether that text names this constant, in English, whatever language the
     * IDE is running in.
     * <p>
     * The constant name is the one spelling of a value that never changes: a
     * label becomes French the day the plugin does, and a file written by a
     * colleague on another language still has to read. Underscores are opened
     * out because a spreadsheet writes "To Be Updated" where the constant says
     * TO_BE_UPDATED, and the two are the same words.
     * <p>
     * Here rather than at the three places that ask - a priority column, a
     * status column and {@code TestEditorAttributes.isColumn} - because a value
     * that imports one way and refuses the other is the same bug three times.
     */
    public static boolean namesConstant(final @NotNull Enum<?> constant, final @NotNull String text) {
        final @NotNull String wanted = text.trim();

        return constant.name().equalsIgnoreCase(wanted) || constant.name().replace('_', ' ').equalsIgnoreCase(wanted);
    }

    /**
     * Reads a priority back out of the text a cell or a sheet holds.
     * <p>
     * By the label first, because that is what the surface printed and therefore
     * what the tester is editing - the same reason {@link #testCaseStatus} looks
     * there first, and the same failure if it does not: the column shows P1 and
     * {@code valueOf} takes the constant name, so retyping the very word on
     * screen would quietly become P3.
     * <p>
     * Then the constant name, which is what a sheet exported before the labels
     * became P1 to P3 still says - High, Medium, Low - so an older export
     * imports as the priority it meant rather than as the fallback.
     * <p>
     * Blank keeps what the case has, because a priority has no empty form - a
     * new case starts at P3 and a cell cleared by hand is a cell that says
     * nothing, not a cell that says P3.
     * <p>
     * A word it cannot read is refused rather than answered with P3. That was
     * the loudest half of #264: importing 200 cases whose priority column reads
     * High, Medium and Low gave 200 at the lowest priority and said nothing.
     */
    public static @NotNull Optional<Priority> priority(final @NotNull String value, final @NotNull Priority current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(current);

        for (final Priority priority : Priority.values()) {
            if (priority.getLabel().equalsIgnoreCase(wanted)) return Optional.of(priority);
            if (namesConstant(priority, wanted)) return Optional.of(priority);
        }

        return Optional.empty();
    }

    /**
     * Reads a test case status back out of the text a grid cell shows.
     * <p>
     * By the display text first, because that is what the cell printed and
     * therefore what the tester is editing. The column used to render
     * {@code getDisplayText()} and parse back with {@code valueOf}, which takes
     * the constant name - so retyping the very word on screen threw an
     * IllegalArgumentException that nothing caught, and the tester got an IDE
     * internal-error report instead of an edit. "To Be Updated" was never a
     * legal answer; "Disabled" was, only because that constant happens to be
     * named in mixed case.
     * <p>
     * Anything unrecognized is refused, the way {@link #priority} refuses. The
     * cell redraws with the old value, so a typo reads as "that did not take"
     * instead of as a crash - and a status is never silently changed to a
     * default the tester did not choose.
     * <p>
     * Blank keeps what the case has, for the same reason a blank priority does:
     * a status has no empty form.
     */
    public static @NotNull Optional<TestCaseStatus> testCaseStatus(final @NotNull String value, final @NotNull TestCaseStatus current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return Optional.of(current);

        for (final TestCaseStatus status : TestCaseStatus.values()) {
            if (status.getLabel().equalsIgnoreCase(wanted)) return Optional.of(status);
            if (namesConstant(status, wanted)) return Optional.of(status);
        }

        return Optional.empty();
    }

    /**
     * Reads a timestamp back out of text, in either shape it is ever written in.
     * <p>
     * The plugin's own is first, because it is the one a tester is looking at:
     * every grid cell, every card and every exported sheet shows
     * "Wednesday 19-08-2026 At 01:12:58 [Asia/Riyadh]", and only the Excel shape
     * was parsed - so exporting a sheet and importing it back read every date as
     * a failure and answered "now". The import preview showed today's date and
     * time for a case created months ago, whatever the file said.
     * <p>
     * A blank cell is the empty timestamp rather than now: the file did not say
     * when, and inventing a moment is what this was doing wrong in the first
     * place. Text that is neither shape is refused - it did say when, and
     * Testin could not read it, which is a different thing from silence.
     */
    public static @NotNull Optional<ZonedDateTime> date(final @NotNull String value) {
        if (value.isBlank()) return Optional.of(Config.NOT_EXECUTED);

        final @NotNull String text = value.trim();
        try {
            // The weekday is dropped before parsing, not matched. It is decoration
            // - derived from the date every time the plugin writes one - and
            // java.time refuses the whole string when the two disagree, which is
            // what an edited cell looks like: "Sunday 05-08-2026" for a date that
            // is a Wednesday. The numbers are the fact, so they are what is read.
            return Optional.of(ZonedDateTime.parse(text.replaceFirst("^\\p{L}+\\s+", ""), WITHOUT_WEEKDAY));
        } catch (final Exception ignored) {
            // Not the plugin's own format; try the plain one a spreadsheet from
            // another tool carries.
        }

        try {
            return Optional.of(LocalDateTime.parse(text, Config.EXCEL_DATE_FORMATTER).atZone(ZoneId.systemDefault()));
        } catch (final Exception unreadable) {
            return Optional.empty();
        }
    }

    /**
     * Reads the groups back out of the text a cell or a sheet holds.
     * <p>
     * Handed to {@link Groups}, which owns what a group is now that it is a word
     * rather than a constant (#296). Nothing here can be refused any more - a
     * name Testin has never seen is a name the tester is adding - so this is the
     * one parsed column that answers with a value every time.
     */
    public static @NotNull Optional<List<String>> groups(final @NotNull String rawGroups) {
        return Optional.of(Groups.read(rawGroups));
    }

}
