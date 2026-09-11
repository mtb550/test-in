package org.testin.model;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import org.testin.util.Bundle;
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

    // The node names below are literals and have to stay that way. DirectoryType
    // names its counts - TRD declares List.of(NodeCount.PACKAGES, ...) - so this
    // enum asking DirectoryType for a caption makes the two initialize each
    // other. It compiles, and it throws ExceptionInInitializerError the first
    // time either is touched.
    //
    // The verdict captions below are safe by the same test: TestStatus names
    // nothing here, and this already read its colours from it.
    TEST_SETS(Bundle.message("count.test.sets"), NodeFigures::testSets, NodeCount::plain, Uncharted.COLOR),
    PACKAGES(Bundle.message("count.packages"), NodeFigures::packages, NodeCount::plain, Uncharted.COLOR),
    /**
     * UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-065.
     * <p>
     * How many test cases lie beneath the node - and, when a new test run would
     * not take all of them, how many it would.
     * <p>
     * Both numbers are right and they answer different questions: a container is
     * the sum of what is beneath it, retired branches included, and a new run
     * leaves retired branches out because that is what retiring one means. With
     * only the first on screen, a test project reading 40 here offered 31 there
     * and nothing said why (#274).
     * <p>
     * Said only when they differ. A node with nothing retired beneath it would
     * otherwise carry the same number twice, which is furniture rather than an
     * answer.
     */
    TEST_CASES(Bundle.message("count.test.cases"), NodeFigures::testCases, NodeCount::plain, Uncharted.COLOR) {
        @Override
        public @NotNull String of(final @NotNull NodeFigures figures) {
            if (figures.testCases() == figures.runnableTestCases()) return super.of(figures);

            return Bundle.message("count.test.cases.runnable", super.of(figures), String.valueOf(figures.runnableTestCases()));
        }
    },
    TEST_RUNS(Bundle.message("count.test.runs"), NodeFigures::testRuns, NodeCount::plain, Uncharted.COLOR),

    PASSED(TestStatus.PASSED.getLabel(), figures -> figures.run().passed(), NodeCount::plain, TestStatus.PASSED.getRowColor()),
    FAILED(TestStatus.FAILED.getLabel(), figures -> figures.run().failed(), NodeCount::plain, TestStatus.FAILED.getRowColor()),
    BLOCKED(TestStatus.BLOCKED.getLabel(), figures -> figures.run().blocked(), NodeCount::plain, TestStatus.BLOCKED.getRowColor()),
    UNTESTED(TestStatus.UNTESTED.getLabel(), figures -> figures.run().untested(), NodeCount::plain, TestStatus.UNTESTED.getRowColor()),
    REMOVED(TestStatus.REMOVED.getLabel(), figures -> figures.run().removed(), NodeCount::plain, TestStatus.REMOVED.getRowColor()),
    TOTAL(Bundle.message("count.total"), figures -> figures.run().total(), NodeCount::plain, Uncharted.COLOR),
    PASS_RATE(Bundle.message("count.pass.rate"), figures -> figures.run().passRate(), NodeCount::percentage, Uncharted.COLOR);

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
