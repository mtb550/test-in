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

package org.testin.model;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.util.Bundle;

import java.awt.Color;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

@Getter
@AllArgsConstructor
public enum NodeCount {
    TEST_SETS(
            Bundle.message("count.test.sets"),
            NodeFigures::testSets,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PACKAGES(
            Bundle.message("count.packages"),
            NodeFigures::packages,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    // UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-065
    TEST_CASES(Bundle.message("count.test.cases"), NodeFigures::testCases, NodeCount::plain, Uncharted.COLOR) {
        @Override
        public @NotNull String of(final @NotNull NodeFigures figures) {
            if (figures.testCases() == figures.runnableTestCases()) return super.of(figures);

            return Bundle.message("count.test.cases.runnable", super.of(figures), String.valueOf(figures.runnableTestCases()));
        }
    },
    TEST_RUNS(
            Bundle.message("count.test.runs"),
            NodeFigures::testRuns,
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PASSED(
            TestStatus.PASSED.getLabel(),
            figures -> figures.run().passed(),
            NodeCount::plain,
            TestStatus.PASSED.getRowColor()
    ),

    FAILED(
            TestStatus.FAILED.getLabel(),
            figures -> figures.run().failed(),
            NodeCount::plain,
            TestStatus.FAILED.getRowColor()
    ),

    BLOCKED(
            TestStatus.BLOCKED.getLabel(),
            figures -> figures.run().blocked(),
            NodeCount::plain,
            TestStatus.BLOCKED.getRowColor()
    ),

    UNTESTED(
            TestStatus.UNTESTED.getLabel(),
            figures -> figures.run().untested(),
            NodeCount::plain,
            TestStatus.UNTESTED.getRowColor()
    ),

    REMOVED(
            TestStatus.REMOVED.getLabel(),
            figures -> figures.run().removed(),
            NodeCount::plain,
            TestStatus.REMOVED.getRowColor()
    ),

    TOTAL(
            Bundle.message("count.total"),
            figures -> figures.run().total(),
            NodeCount::plain,
            Uncharted.COLOR
    ),

    PASS_RATE(
            Bundle.message("count.pass.rate"),
            figures -> figures.run().passRate(),
            NodeCount::percentage,
            Uncharted.COLOR
    );

    private final @NotNull String caption;
    private final @NotNull ToLongFunction<NodeFigures> reader;
    private final @NotNull LongFunction<String> format;

    private final @NotNull Color swatch;

    private static @NotNull String plain(final long value) {
        return String.valueOf(value);
    }

    private static @NotNull String percentage(final long value) {
        return value + "%";
    }

    public @NotNull String of(final @NotNull NodeFigures figures) {
        return format.apply(reader.applyAsLong(figures));
    }

    public long valueIn(final @NotNull NodeFigures figures) {
        return reader.applyAsLong(figures);
    }

    private static final class Uncharted {
        private static final @NotNull Color COLOR = JBColor.lazy(UIUtil::getContextHelpForeground);
    }
}
