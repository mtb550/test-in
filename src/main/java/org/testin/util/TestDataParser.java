package org.testin.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;
import org.testin.model.Group;
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

    public static @NotNull Priority priority(final @NotNull String value) {
        if (value.isBlank()) return Priority.LOW;
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return Priority.LOW;
        }
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
     * Anything unrecognized keeps the status the case already has, the way
     * {@link #priority} falls back rather than throwing. The cell redraws with
     * the old value, so a typo reads as "that did not take" instead of as a
     * crash - and a status is never silently changed to a default the tester
     * did not choose.
     */
    public static @NotNull TestCaseStatus testCaseStatus(final @NotNull String value, final @NotNull TestCaseStatus current) {
        final @NotNull String wanted = value.trim();
        if (wanted.isEmpty()) return current;

        for (final TestCaseStatus status : TestCaseStatus.values()) {
            if (status.getDisplayText().equalsIgnoreCase(wanted)) return status;
            if (status.name().equalsIgnoreCase(wanted)) return status;
        }

        return current;
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
     * A blank cell, or text that is neither shape, is the empty timestamp rather
     * than now: the file did not say when, and inventing a moment is what this
     * was doing wrong in the first place.
     */
    public static @NotNull ZonedDateTime date(final @NotNull String value) {
        if (value.isBlank()) return Config.NOT_EXECUTED;

        final @NotNull String text = value.trim();
        try {
            // The weekday is dropped before parsing, not matched. It is decoration
            // - derived from the date every time the plugin writes one - and
            // java.time refuses the whole string when the two disagree, which is
            // what an edited cell looks like: "Sunday 05-08-2026" for a date that
            // is a Wednesday. The numbers are the fact, so they are what is read.
            return ZonedDateTime.parse(text.replaceFirst("^\\p{L}+\\s+", ""), WITHOUT_WEEKDAY);
        } catch (final Exception ignored) {
            // Not the plugin's own format; try the plain one a spreadsheet from
            // another tool carries.
        }

        try {
            return LocalDateTime.parse(text, Config.EXCEL_DATE_FORMATTER)
                    .atZone(ZoneId.systemDefault());
        } catch (final Exception ignored) {
            return Config.NOT_EXECUTED;
        }
    }

    public static @NotNull List<Group> groups(final @NotNull String rawGroups) {
        if (rawGroups.isBlank()) return new ArrayList<>();
        return Arrays.stream(rawGroups.split(","))
                .map(String::trim)
                .filter(group -> !group.isEmpty())
                .map(group -> {
                    try {
                        return Group.valueOf(group.toUpperCase(Locale.ROOT));
                    } catch (final IllegalArgumentException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
