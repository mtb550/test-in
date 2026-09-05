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
     * How long the tester has been on this case, as a clock that is running:
     * minutes and seconds, and hours only once there are some.
     * <p>
     * <b>Always a number, which is the difference from {@link #formatDuration}.</b>
     * A case in front of a tester has been going for however long they have been
     * looking at it, so a zero here means "just started" - and a clock that blinks
     * out for its first second is telling them about the formatter rather than
     * about the case. A recorded zero means something else entirely, which is why
     * the stored value is a separate method.
     */
    public static @NotNull String formatCaseClock(final @NotNull Duration duration) {
        final @NotNull String minutes = String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());

        return duration.toHours() == 0 ? minutes : duration.toHours() + ":" + minutes;
    }

    /**
     * How long a run has been going: hours, minutes and seconds, with the hours
     * always in front.
     * <p>
     * A run is the length of a testing session and a tester wants the hours on
     * it, so the field is there from the start rather than appearing at 01:00:00
     * - a number that grows a field is a number that jumps. Both places that show
     * a run total read it here: the run editor's own status bar and light mode.
     * <p>
     * <b>Always carrying the hours is also what keeps light mode's strip
     * readable.</b> Its two clocks have no labels, because the right-hand figure
     * is always the larger of the two and that says which is which faster than a
     * word would. Showing the run in hours and minutes alone broke exactly that:
     * a run at five minutes read 00:05 beside a case at four and a half reading
     * 04:30, and the smaller number was the longer time. With the seconds kept
     * and the hours always present, the run clock is the wider and the larger of
     * the pair whatever either of them holds.
     * <p>
     * Blank for a run nobody has started, which is the rule the editor's status
     * bar already draws on - it hides the label when there is nothing to show.
     */
    public static @NotNull String formatRunClock(final @NotNull Duration duration) {
        return duration.isZero() ? "" : String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    /**
     * A case's recorded duration, as the grid and an exported sheet show it: the
     * same minutes and seconds {@link #formatCaseClock} draws, and blank when
     * nothing was measured.
     * <p>
     * Zero is not a case that took no time - it is a case the timer never ran
     * for, because the verdict came from the context menu, a bulk apply or the
     * failure dialog. Printing 00:00 for those claims a measurement nobody took.
     * That guard is the whole of what this adds to the clock it delegates to.
     * <p>
     * <b>No milliseconds, on any surface.</b> They are still measured and still
     * stored - what is written to disk keeps every one of them - and they are
     * simply not drawn. This used to print "84ms" under a second and a ".400"
     * tail above it, on the argument that a framework-timed case reporting 84ms
     * should not read as no time at all. What that cost was three digits on every
     * hand-timed case that changed nothing a tester could act on, and a column
     * whose shape depended on how fast the case had been. An automated case under
     * a second now reads 00:00 here, and its measurement is in the file.
     */
    public static @NotNull String formatDuration(final @NotNull Duration duration) {
        return duration.isZero() ? "" : formatCaseClock(duration);
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
