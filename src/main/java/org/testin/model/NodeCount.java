package org.testin.model;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

/**
 * One labelled number a node reports in its Details.
 * <p>
 * Each constant knows its caption, which field of {@link NodeFigures} it reads,
 * how that field is written - a count is a count, a rate carries its sign - and
 * the color it draws as when it is drawn. So the dialog renders any of them the
 * same way and knows none of them by name, which is the same reason
 * {@link TestStatus} carries its own icon and key rather than letting a menu
 * decide.
 * <p>
 * Which of these a node shows is declared by {@link DirectoryType}, not chosen
 * here: a test set has no runs beneath it and a run has no sets, and those are
 * impossible states rather than zeroes worth printing.
 */
@Getter
@AllArgsConstructor
public enum NodeCount {
    TEST_SETS("Test sets", NodeFigures::testSets, NodeCount::plain, Uncharted.COLOR),
    PACKAGES("Packages", NodeFigures::packages, NodeCount::plain, Uncharted.COLOR),
    TEST_CASES("Test cases", NodeFigures::testCases, NodeCount::plain, Uncharted.COLOR),
    TEST_RUNS("Test runs", NodeFigures::testRuns, NodeCount::plain, Uncharted.COLOR),

    PASSED("Passed", figures -> figures.run().passed(), NodeCount::plain, TestStatus.PASSED.getRowColor()),
    FAILED("Failed", figures -> figures.run().failed(), NodeCount::plain, TestStatus.FAILED.getRowColor()),
    BLOCKED("Blocked", figures -> figures.run().blocked(), NodeCount::plain, TestStatus.BLOCKED.getRowColor()),
    UNTESTED("Untested", figures -> figures.run().untested(), NodeCount::plain, TestStatus.UNTESTED.getRowColor()),
    REMOVED("Removed", figures -> figures.run().removed(), NodeCount::plain, TestStatus.REMOVED.getRowColor()),
    TOTAL("Total", figures -> figures.run().total(), NodeCount::plain, Uncharted.COLOR),
    PASS_RATE("Pass rate", figures -> figures.run().passRate(), NodeCount::percentage, Uncharted.COLOR);

    private final @NotNull String caption;
    private final @NotNull ToLongFunction<NodeFigures> reader;
    private final @NotNull LongFunction<String> format;

    /**
     * The color this count draws as in a chart.
     * <p>
     * The verdicts take theirs from {@link TestStatus}, which is what the grid
     * rows, the card badges and the tree already draw with, so a green arc and
     * a green row mean the same thing without a second palette to keep in step.
     */
    private final @NotNull Color swatch;

    /**
     * What this count reads as for these figures, ready for a row.
     */
    public @NotNull String of(final @NotNull NodeFigures figures) {
        return format.apply(reader.applyAsLong(figures));
    }

    /**
     * The raw number, for a caller that draws it rather than prints it.
     */
    public long valueIn(final @NotNull NodeFigures figures) {
        return reader.applyAsLong(figures);
    }

    /**
     * The color of a count no chart draws.
     * <p>
     * Held here rather than beside the other fields because a constant of the
     * enum may not name a static field of the enum, and lazy because it comes
     * from the theme: resolved at class-load time it would keep the color of
     * whichever theme happened to be active then.
     */
    private static final class Uncharted {

        private static final @NotNull Color COLOR = JBColor.lazy(UIUtil::getContextHelpForeground);
    }

    private static @NotNull String plain(final long value) {
        return String.valueOf(value);
    }

    private static @NotNull String percentage(final long value) {
        return value + "%";
    }
}
