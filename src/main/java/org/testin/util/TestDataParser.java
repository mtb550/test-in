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
            if (priority.name().equalsIgnoreCase(wanted)) return Optional.of(priority);
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
            if (status.name().equalsIgnoreCase(wanted)) return Optional.of(status);
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
     * Refused whole rather than in part. A cell reading "Regression, Nonsense"
     * used to keep Regression and drop the rest without a word, so a tester who
     * mistyped one group in a list of four got three and no sign that the fourth
     * had gone (#264). The cell is one value the tester typed, and one value is
     * kept or refused.
     * <p>
     * Blank is the empty list, which is what every reader already treats as
     * unassigned - a group has an empty form, so blank clears it.
     */
    public static @NotNull Optional<List<Group>> groups(final @NotNull String rawGroups) {
        if (rawGroups.isBlank()) return Optional.of(new ArrayList<>());

        // The picker offers No Group and writes the label it draws, so this has
        // to read it back. It used to reach Group.valueOf, throw on the angle
        // brackets and be dropped as an unknown group - a value the plugin
        // itself offered, silently thrown away (#265). No group is no groups,
        // which is the empty list every reader already treats as unassigned.
        if (rawGroups.trim().equalsIgnoreCase(Group.UNASSIGNED.getName())) return Optional.of(new ArrayList<>());

        final @NotNull List<Group> read = new ArrayList<>();
        for (final String name : rawGroups.split(",")) {
            final @NotNull String wanted = name.trim();
            if (wanted.isEmpty()) continue;

            final @NotNull Optional<Group> group = Arrays.stream(Group.values())
                    .filter(one -> one.name().equalsIgnoreCase(wanted) || one.getName().equalsIgnoreCase(wanted))
                    .findFirst();

            if (group.isEmpty()) return Optional.empty();

            read.add(group.get());
        }

        return Optional.of(read);
    }

}
