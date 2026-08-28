package org.testin.model;

import com.intellij.ui.JBColor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.ToLongFunction;

/**
 * The four verdicts a tester writes about after a run, and what a report calls
 * each of them.
 * <p>
 * The counts stay where they were - read from {@link TestRunSummary}, so the
 * heading over a paragraph and the table further down cannot disagree about how
 * many cases passed. What is new is the paragraph: the numbers say what happened
 * and only a tester can say why, and that sentence used to live in whatever
 * document they wrote by hand afterwards.
 * <p>
 * One declaration for all three report formats and the dialog that fills them
 * in. The color is a hex string rather than a format's own color type because
 * PDF, Word and HTML each have their own and all three already used these exact
 * values.
 */
@Getter
@AllArgsConstructor
public enum ResultAnalysis {

    PASSED(TestStatus.PASSED, TestStatus.PASSED, "2E7D32", "4FBF60", TestRunSummary::passed),
    FAILED(TestStatus.FAILED, TestStatus.FAILED, "C0392B", "F2685A", TestRunSummary::failed),
    BLOCKED(TestStatus.BLOCKED, TestStatus.BLOCKED, "B8860B", "F5B940", TestRunSummary::blocked),

    /**
     * The one bucket with two names, because it is one fact at two moments.
     * <p>
     * A case nobody has reached is {@code PENDING} while the run is open and
     * {@code UNTESTED} once the run completes or closes - the run changes it
     * itself, as {@link TestStatus#UNTESTED} records. {@link TestRunSummary}
     * counts the two together for exactly that reason, and this constant is
     * where the counting stops and the naming has to choose.
     * <p>
     * The constant is still called UNTESTED because its name is persisted: a
     * tester's written analysis is stored keyed by it, and renaming it would
     * make every stored analysis unreadable.
     */
    UNTESTED(TestStatus.PENDING, TestStatus.UNTESTED, "595959", "96A1B0", TestRunSummary::untested);

    /**
     * What this bucket is called while the run is still open, and what it is
     * called once the run has finished. The same constant twice for the three
     * verdicts a tester gives, which do not change their name.
     */
    private final @NotNull TestStatus whileRunning;
    private final @NotNull TestStatus onceFinished;

    /**
     * The color a report paints this verdict, and the only one a report may use:
     * a report is read on white, whatever theme produced it.
     */
    private final @NotNull String hexColor;

    /**
     * The same verdict for a dark IDE, where the report color is too dark to
     * read - {@code 595959} on a dark editor background is very nearly the
     * background. These are the lighter values the report sections already
     * carried for their own charts, so the two sets are not a new invention.
     */
    private final @NotNull String darkHexColor;

    private final @NotNull ToLongFunction<TestRunSummary> count;

    /**
     * What a finished run calls this bucket.
     * <p>
     * The finished name rather than a choice, because everything that asks for a
     * bare label is looking at a run that is over: the Result Analysis dialog
     * refuses to open until the run is completed, and the paragraph it collects
     * is what the reports print. A live view wants {@link #labelIn} instead.
     */
    public @NotNull String getLabel() {
        return onceFinished.getLabel();
    }

    /**
     * What this bucket is called with the run in this state.
     * <p>
     * A status bar watching a run in progress must not call its untouched cases
     * "Untested": they are pending, the run has not given up on them, and the
     * word only becomes true when the run completes or closes.
     */
    public @NotNull String labelIn(final @NotNull TestRunStatus run) {
        return (run.isTerminal() ? onceFinished : whileRunning).getLabel();
    }

    /**
     * The heading over the paragraph: the verdict and how many cases carry it.
     */
    public @NotNull String heading(final @NotNull TestRunSummary summary) {
        return getLabel() + " (" + count.applyAsLong(summary) + ")";
    }

    /**
     * How the run went on one line, for a status bar that has room for one.
     * <p>
     * Here rather than on {@link TestRunSummary} because this is the enum that
     * already pairs a verdict with its name and its count, and a second pairing
     * is how the reports drifted apart the first time. A label renamed on
     * {@link TestStatus} therefore renames it in the reports, the analysis
     * dialog and the status bar together.
     * <p>
     * A verdict no case carries is left out, the rule {@link
     * TestRunSummary#hasRemoved()} already states: a run that blocked nothing
     * should not carry "Blocked 0" to explain something that did not happen. So
     * a run nobody has judged says nothing at all, which is what the execution
     * time beside it does with a duration of zero.
     * <p>
     * Removed is counted here although it is not one of the four - the total on
     * the other side of the bar counts those cases, so a line that left them out
     * would not add up to the number the tester is reading beside it.
     * <p>
     * The run's status is handed in because one of the four buckets is named for
     * it: see {@link #labelIn}.
     */
    public static @NotNull String headline(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus run) {
        final @NotNull StringJoiner line = new StringJoiner(" · ");

        for (final ResultAnalysis section : values()) {
            final long cases = section.count.applyAsLong(summary);
            if (cases > 0) line.add(section.painted(section.labelIn(run) + " " + cases));
        }

        // Uncolored on purpose: a case deleted out from under the run is not a
        // verdict anybody reached, and painting it like one would say it was.
        if (summary.hasRemoved()) line.add(TestStatus.REMOVED.getLabel() + " " + summary.removed());

        // <nobr>, because a Swing label rendering html wraps its text to whatever
        // width it is given - and given a narrow bar it broke this line in two and
        // drew half of it below the other, inside a strip one line tall. Nothing
        // here is a sentence: it is one row of figures, and a row that does not
        // fit should be cut off at the edge rather than folded under itself.
        return line.length() == 0 ? "" : "<html><nobr>" + line + "</nobr></html>";
    }

    /**
     * The text in this verdict's color, for a Swing label that renders HTML.
     * <p>
     * The theme is read at the moment the line is built rather than baked in,
     * because the same enum serves a report that is always read on white and a
     * status bar that is read on whichever background the tester chose.
     */
    private @NotNull String painted(final @NotNull String text) {
        return "<span style=\"color:#" + (JBColor.isBright() ? hexColor : darkHexColor) + "\">" + text + "</span>";
    }

    /**
     * What the tester wrote about this verdict, and empty when they wrote
     * nothing - which is the whole of the rule the reports follow: a verdict
     * nobody commented on is not printed.
     */
    public @NotNull String writtenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        return analysis.getOrDefault(this, "").trim();
    }

    /**
     * Whether the tester wrote anything at all. A run nobody analysed prints no
     * Result Analysis section, rather than a heading over four empty ones.
     */
    public static boolean anyWrittenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        for (final ResultAnalysis section : values()) {
            if (!section.writtenIn(analysis).isEmpty()) return true;
        }

        return false;
    }

    /**
     * The sections worth storing: the ones the tester actually wrote in.
     * <p>
     * A section left alone is absent from the file rather than present and
     * empty. Every reader already treats the two the same - {@link #writtenIn}
     * answers blank either way - so this changes nothing but what is on disk,
     * where four empty strings under a heading nobody filled in are noise a
     * colleague reads in a diff.
     * <p>
     * What is kept is kept as typed. Whitespace-only counts as nothing written,
     * because that is what {@code writtenIn} already decides - but a value with
     * text in it is stored exactly as the tester left it, since saving does not
     * reformat.
     */
    public static @NotNull Map<ResultAnalysis, String> written(final @NotNull Map<ResultAnalysis, String> analysis) {
        final @NotNull Map<ResultAnalysis, String> kept = new EnumMap<>(ResultAnalysis.class);

        for (final ResultAnalysis section : values()) {
            if (section.writtenIn(analysis).isEmpty()) continue;

            kept.put(section, Objects.toString(analysis.get(section), ""));
        }

        return kept;
    }
}
