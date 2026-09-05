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
     * A timestamp as everything shows it: the card, the grid, the details popup,
     * an exported sheet, a report footer - and blank when there is no moment to
     * show, which {@link Config#NOT_EXECUTED} is how the model says it.
     */
    public static @NotNull String formatDate(final @NotNull ZonedDateTime at) {
        return Config.isNotExecuted(at) ? "" : at.format(Config.getDateFormatterPattern());
    }

    /**
     * The steps, numbered the way this plugin numbers them, one per line.
     * <p>
     * A step is numbered by where it sits in the case, so the number a tester
     * reads here is the number they see in the editor. A blank step is not
     * drawn and its number is not reused, which is why a case with a gap in it
     * reads 1, 2, 4 - the fourth step really is the fourth.
     * <p>
     * Here rather than beside either of the two things that draw steps - the
     * details panel, which gives each step its own row, and light mode, which
     * shows them as one paragraph - because "1- " is a decision about how a
     * value reads, and this class owns those.
     */
    public static @NotNull String numberedSteps(final @NotNull List<String> steps) {
        final @NotNull StringBuilder text = new StringBuilder();

        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).isBlank()) continue;

            if (!text.isEmpty()) text.append("\n");
            text.append(numberedStep(i, steps.get(i)));
        }

        return text.toString();
    }

    /**
     * One step, numbered from its position in the list.
     */
    public static @NotNull String numberedStep(final int index, final @NotNull String step) {
        return (index + 1) + "- " + format(step);
    }

    /**
     * How long the tester has been on this case: minutes and seconds, and hours
     * only once there are some.
     * <p>
     * <b>Not {@link #formatDuration}, and the difference is the point.</b> That
     * one is the measurement - it prints milliseconds, drops to "84ms" under a
     * second, and is blank when nothing was measured. All three are right for a
     * stored result and wrong for a number ticking in front of a tester: a clock
     * that blinks out under a second, or reads 00:00:41 for forty-one seconds,
     * is telling them about the formatter rather than about the case.
     * <p>
     * Seconds, because this is the figure that moves while somebody watches it.
     * The duration still arrives measured to the millisecond and the digits are
     * dropped at the last moment, so nothing is lost from what gets stored - and
     * nothing here reaches a report or an exported sheet, which keep every
     * measurement they were given.
     */
    public static @NotNull String formatCaseClock(final @NotNull Duration duration) {
        final @NotNull String minutes = String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());

        return duration.toHours() == 0 ? minutes : duration.toHours() + ":" + minutes;
    }

    /**
     * How long the run has been going: hours and minutes, and no seconds.
     * <p>
     * A test run is measured in the time somebody spends on it, and at that
     * length the seconds are two digits that change every second and say nothing
     * - they take the number's width with them and pull the eye to the corner of
     * a window built to hold one sentence. The case clock beside it keeps its
     * seconds, because that is the figure a tester is actually watching move.
     * <p>
     * Display only, and only where a clock is shown. What is recorded against
     * the run is untouched, so the report and the details popup still say
     * exactly when it started and ended.
     */
    public static @NotNull String formatRunClock(final @NotNull Duration duration) {
        return String.format("%02d:%02d", duration.toHours(), duration.toMinutesPart());
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

        // Under a second reads as milliseconds, because the clock is no longer
        // the only source. A tester working through a case by hand is timed to
        // the second and never finishes one inside a second; a test framework
        // measures the method itself and routinely reports 84ms - which this
        // rendered as 00:00:00, a duration that reads as "no time at all" for
        // something that did take time.
        if (duration.toSeconds() == 0) return duration.toMillis() + "ms";

        final @NotNull String clock = String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());

        // The milliseconds only when there are some. Whole seconds are what a
        // case timed by hand mostly comes to, and 00:03:07.000 is three extra
        // digits that say nothing; a case that took 1.4 seconds used to read
        // 00:00:01, which said something untrue.
        return duration.toMillisPart() == 0 ? clock : clock + String.format(".%03d", duration.toMillisPart());
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
