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

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.function.ToLongFunction;

@Getter
@AllArgsConstructor
public enum ResultAnalysis {
    PASSED(
            TestStatus.PASSED,
            TestStatus.PASSED,
            "2E7D32",
            "4FBF60",
            TestRunSummary::passed
    ),

    FAILED(
            TestStatus.FAILED,
            TestStatus.FAILED,
            "C0392B",
            "F2685A",
            TestRunSummary::failed
    ),

    BLOCKED(
            TestStatus.BLOCKED,
            TestStatus.BLOCKED,
            "B8860B",
            "F5B940",
            TestRunSummary::blocked
    ),

    UNTESTED(
            TestStatus.PENDING,
            TestStatus.UNTESTED,
            "595959",
            "96A1B0",
            TestRunSummary::untested
    );

    private final @NotNull TestStatus whileRunning;
    private final @NotNull TestStatus onceFinished;

    private final @NotNull String hexColor;

    private final @NotNull String darkHexColor;

    private final @NotNull ToLongFunction<TestRunSummary> count;

    public @NotNull String getLabel() {
        return onceFinished.getLabel();
    }

    public @NotNull String labelIn(final @NotNull TestRunStatus run) {
        return (run.isTerminal() ? onceFinished : whileRunning).getLabel();
    }

    public @NotNull String heading(final @NotNull TestRunSummary summary) {
        return getLabel() + " (" + count.applyAsLong(summary) + ")";
    }

    public static @NotNull List<Segment> segments(final @NotNull TestRunSummary summary, final @NotNull TestRunStatus run) {
        final @NotNull List<Segment> segments = new ArrayList<>();

        for (final ResultAnalysis section : values()) {
            final long cases = section.count.applyAsLong(summary);
            if (cases > 0) segments.add(new Segment(section.labelIn(run) + " " + cases, section.onScreen()));
        }

        if (summary.hasRemoved()) {
            segments.add(new Segment(TestStatus.REMOVED.getLabel() + " " + summary.removed(), UIUtil.getInactiveTextColor()));
        }

        return segments;
    }

    public record Segment(@NotNull String text, @NotNull Color color) {
    }

    private @NotNull JBColor onScreen() {
        return new JBColor(Color.decode("#" + hexColor), Color.decode("#" + darkHexColor));
    }

    public @NotNull String writtenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        return analysis.getOrDefault(this, "").trim();
    }

    public static boolean anyWrittenIn(final @NotNull Map<ResultAnalysis, String> analysis) {
        for (final ResultAnalysis section : values()) {
            if (!section.writtenIn(analysis).isEmpty()) return true;
        }

        return false;
    }

    public static @NotNull Map<ResultAnalysis, String> written(final @NotNull Map<ResultAnalysis, String> analysis) {
        final @NotNull Map<ResultAnalysis, String> kept = new EnumMap<>(ResultAnalysis.class);

        for (final ResultAnalysis section : values()) {
            if (section.writtenIn(analysis).isEmpty()) continue;

            kept.put(section, Objects.toString(analysis.get(section), ""));
        }

        return kept;
    }
}
