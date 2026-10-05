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

package org.testin.indexer;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.NodeFigures;
import org.testin.model.result.TestRunSummary;
import org.testin.model.node.DirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCounter {
    public static @NotNull NodeFigures figures(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        return Gathered.valueOf(dto.getType().getStatistics().name()).getGather().of(p, dto);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-047, Rule-INTERNAL-050
    public static @NotNull NodeFigures childCounts(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        final @NotNull TestCases testCases = Services.getInstance(p, TestCases.class);
        final @NotNull Nodes nodes = Services.getInstance(p, Nodes.class);
        final @NotNull List<DirectoryDto> beneath = beneath(nodes, dto);

        final @NotNull Map<DirectoryType, Long> byType = beneath.stream()
                .collect(Collectors.groupingBy(DirectoryDto::getType, Collectors.counting()));

        return NodeFigures.ofChildren(
                counted(byType, DirectoryType.TS),
                counted(byType, DirectoryType.TSP) + counted(byType, DirectoryType.TRP),
                testCases.testCaseCountOf(dto.getPath())
                        + beneath.stream().mapToLong(node -> testCases.testCaseCountOf(node.getPath())).sum(),
                activeTestCasesUnder(testCases, nodes, dto),
                counted(byType, DirectoryType.TR));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-048, Rule-INTERNAL-049, Rule-INTERNAL-051
    public static @NotNull NodeFigures testRunFigures(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        return Services.getInstance(p, TestRuns.class).findTestRun(dto.getPath())
                .map(testRun -> NodeFigures.ofTestRun(TestRunSummary.of(testRun.getResults())))
                .orElse(NodeFigures.NONE);
    }

    private static @NotNull List<DirectoryDto> beneath(final @NotNull Nodes nodes, final @NotNull DirectoryDto node) {
        final @NotNull List<DirectoryDto> found = new ArrayList<>();

        for (final DirectoryDto child : nodes.getChildren(node.getPath())) {
            found.add(child);
            found.addAll(beneath(nodes, child));
        }

        return found;
    }

    private static long activeTestCasesUnder(final @NotNull TestCases testCases, final @NotNull Nodes nodes, final @NotNull DirectoryDto node) {
        long count = testCases.testCaseCountOf(node.getPath());

        for (final DirectoryDto child : nodes.getChildren(node.getPath())) {
            if (!child.isRetired()) count += activeTestCasesUnder(testCases, nodes, child);
        }

        return count;
    }

    private static long counted(final @NotNull Map<DirectoryType, Long> byType, final @NotNull DirectoryType type) {
        return byType.getOrDefault(type, 0L);
    }
}
