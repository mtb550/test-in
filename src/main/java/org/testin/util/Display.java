package org.testin.util;

import com.intellij.openapi.util.text.StringUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.Config;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * How a stored value is shown to a tester - and only that.
 * <p>
 * Rendering may reformat a value; saving never does. Nothing here is ever
 * written back into a file: the editable surfaces load the raw value, so
 * formatted text cannot be committed into storage.
 * <p>
 * One owner per shape, because each of these is a contract rather than a style.
 * The date proved it: the exporter wrote one shape while the parser knew
 * another, so every re-imported date failed and became "now" (#66).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Display {

    /**
     * A list of things as a person would say it: "a", "a and b", "a, b and c".
     * <p>
     * Here because a dialog naming what it is about to erase reads as a
     * sentence, and "the actual result, the stacktrace" reads as a log line.
     * Same reason as everything else in this class - the shape a value is shown
     * in has one owner.
     */
    public static @NotNull String andJoin(final @NotNull List<String> parts) {
        if (parts.isEmpty()) return "";
        if (parts.size() == 1) return parts.getFirst();

        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.getLast();
    }

    /**
     * A value as the details panel shows it: capitalized, and ended with a full
     * stop unless it already ends in something that closes it (#22).
     */
    public static @NotNull String format(final @NotNull String text) {
        if (text.isBlank()) return "";

        final @NotNull String s = text.trim();
        return StringUtil.capitalize(s) + (endsClosed(s) ? "" : ".");
    }

    /**
     * A comma-separated line - "field: value, field: value" - broken so each
     * entry sits on its own line, and nothing else: no capital, no closing dot.
     * Test data is written this way and read a field at a time, so both the
     * details panel and the grid cell break it where the commas are and show it
     * otherwise verbatim - the two surfaces match, and neither reformats what the
     * tester typed. Display only: the stored value keeps its single line, so the
     * grid cell editor still round-trips it unchanged.
     */
    public static @NotNull String entriesOnLines(final @NotNull String text) {
        return text.replaceAll(",\\s*", "\n");
    }

    /**
     * A timestamp as everything shows it: the card, the grid, the details popup,
     * an exported sheet, a report footer - and blank when there is no moment to
     * show, which {@link Config#NOT_EXECUTED} is how the model says it.
     */
    public static @NotNull String formatDate(final @NotNull ZonedDateTime at) {
        return Config.isNotExecuted(at) ? "" : at.format(Config.getDateFormatterPattern());
    }

    /**
     * A measured duration as HH:MM:SS, and blank when nothing was measured.
     * <p>
     * Zero is not a case that took no time - it is a case the timer never ran
     * for, because the verdict came from the context menu, a bulk apply or the
     * failure dialog. Printing 00:00 for those claims a measurement nobody took.
     */
    public static @NotNull String formatDuration(final @NotNull Duration duration) {
        if (duration.isZero()) return "";

        return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    /**
     * True when a full stop would be wrong: the text already ends in
     * punctuation, or in a character that means it is not a sentence - a URL or
     * a path ending in '/', a parenthesized note, a code snippet.
     */
    private static boolean endsClosed(final @NotNull String s) {
        return ".!?:;/)".indexOf(s.charAt(s.length() - 1)) >= 0;
    }
}
