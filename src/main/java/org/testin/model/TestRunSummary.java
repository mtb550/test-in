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

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record TestRunSummary(long total, long passed, long failed, long blocked, long untested, long removed, @NotNull String executedBy) {
    public static final @NotNull TestRunSummary EMPTY = new TestRunSummary(0, 0, 0, 0, 0, 0, "");

    // UC-INTERNAL-006, Rule-INTERNAL-048, Rule-INTERNAL-049
    public static @NotNull TestRunSummary of(final @NotNull List<TestRunItems> results) {
        final @NotNull Map<TestStatus, Long> counts = results.stream()
                .collect(Collectors.groupingBy(TestRunItems::shownStatus, Collectors.counting()));

        return new TestRunSummary(
                results.size(),
                counts.getOrDefault(TestStatus.PASSED, 0L),
                counts.getOrDefault(TestStatus.FAILED, 0L),
                counts.getOrDefault(TestStatus.BLOCKED, 0L),
                counts.getOrDefault(TestStatus.PENDING, 0L) + counts.getOrDefault(TestStatus.UNTESTED, 0L),
                counts.getOrDefault(TestStatus.REMOVED, 0L),
                whoExecuted(results));
    }

    private static @NotNull String whoExecuted(final @NotNull List<TestRunItems> results) {
        return results.stream()
                .map(TestRunItems::getExecutedBy)
                .filter(name -> !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public boolean hasRemoved() {
        return removed > 0;
    }

    public long executed() {
        return passed + failed + blocked;
    }

    // UC-INTERNAL-006, Rule-INTERNAL-049
    public int passRate() {
        return executed() > 0 ? Math.round((float) passed * 100 / executed()) : 0;
    }
}
