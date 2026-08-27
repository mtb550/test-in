package org.testin.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * How this plugin writes a moment down, and the moment that never came.
 * <p>
 * Constants only. It held a cached Java test source root too, which is a
 * project's answer rather than a constant - see {@code TestSourceRoot}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Config {
    public static final @NotNull DateTimeFormatter EXCEL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final @NotNull String DATE_FORMAT_PATTERN = "EEEE dd-MM-yyyy 'At' HH:mm:ss '['VV']'";
    /**
     * The empty timestamp: something that has not happened yet. A case nobody has
     * given a verdict, a run nobody has started - their timestamps hold this rather
     * than "now" or a null. What {@code BugSeverity.EMPTY} is to a bug that was never
     * recorded, this is to a moment that never came.
     */
    public static final @NotNull ZonedDateTime NOT_EXECUTED = Instant.EPOCH.atZone(ZoneOffset.UTC);
    @Getter
    private static final @NotNull DateTimeFormatter dateFormatterPattern = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.US);
    /**
     * Compared as instants, not with {@code ZonedDateTime.equals}: the mapper moves
     * every timestamp it reads into the system zone, so the epoch comes back from a
     * run file as 03:00 in Asia/Riyadh and equals() would call that a different moment.
     */
    public static boolean isNotExecuted(final @NotNull ZonedDateTime at) {
        return at.toInstant().equals(NOT_EXECUTED.toInstant());
    }

}