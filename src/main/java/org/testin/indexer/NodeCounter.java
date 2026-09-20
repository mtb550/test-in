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
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.model.DirectoryType;
import org.testin.model.NodeFigures;
import org.testin.model.NodeStatistics;
import org.testin.model.TestRunSummary;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NodeCounter {
    @AllArgsConstructor
    private enum Gathered {
        CHILDREN(
                NodeCounter::childCounts
        ),

        VERDICTS(
                NodeCounter::runVerdicts
        );

        private final @NotNull FiguresGatherer gather;
    }

    public static @NotNull NodeFigures figures(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        return Gathered.valueOf(dto.getType().getStatistics().name()).gather.of(p, dto);
    }

    // UC-INTERNAL-006, Rule-INTERNAL-046, Rule-INTERNAL-047, Rule-INTERNAL-050
    public static @NotNull NodeFigures childCounts(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull List<DirectoryDto> beneath = beneath(indexer, dto);

        final @NotNull Map<DirectoryType, Long> byType = beneath.stream()
                .collect(Collectors.groupingBy(DirectoryDto::getType, Collectors.counting()));

        return NodeFigures.ofChildren(
                counted(byType, DirectoryType.TS),
                counted(byType, DirectoryType.TSP) + counted(byType, DirectoryType.TRP),
                indexer.caseCountOf(dto.getPath())
                        + beneath.stream().mapToLong(node -> indexer.caseCountOf(node.getPath())).sum(),
                indexer.getTestCasesUnder(dto).size(),
                counted(byType, DirectoryType.TR));
    }

    // UC-INTERNAL-006, Rule-INTERNAL-048, Rule-INTERNAL-049, Rule-INTERNAL-051
    public static @NotNull NodeFigures runVerdicts(final @NotNull Project p, final @NotNull DirectoryDto dto) {
        return Services.getInstance(p, ProjectIndexer.class).findTestRun(dto.getPath())
                .map(run -> NodeFigures.ofRun(TestRunSummary.of(run.getResults())))
                .orElse(NodeFigures.NONE);
    }

    private static @NotNull List<DirectoryDto> beneath(final @NotNull ProjectIndexer indexer, final @NotNull DirectoryDto node) {
        final @NotNull List<DirectoryDto> found = new ArrayList<>();

        for (final DirectoryDto child : indexer.getChildren(node.getPath())) {
            found.add(child);
            found.addAll(beneath(indexer, child));
        }

        return found;
    }

    private static long counted(final @NotNull Map<DirectoryType, Long> byType, final @NotNull DirectoryType type) {
        return byType.getOrDefault(type, 0L);
    }
}
