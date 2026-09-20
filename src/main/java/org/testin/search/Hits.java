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

package org.testin.search;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.testcase.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hits {
    private static final int SHOWN = 50;

    // UC-INTERNAL-001, Rule-INTERNAL-001
    public static @NotNull Found forQuery(final @NotNull Project p, final @NotNull String query) {
        final @NotNull String wanted = query.trim().toLowerCase(Locale.ROOT);
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        final @NotNull List<DirectoryDto> nodes = wanted.isEmpty() ? everywhereToGo(indexer) : nodesNamed(indexer, wanted);
        final @NotNull List<TestCaseDto> cases = casesMatching(indexer, wanted);

        final @NotNull List<Hit> found = new ArrayList<>(nodes.stream().limit(SHOWN).map(Hit::of).toList());
        found.addAll(topCases(cases, wanted, SHOWN - found.size()));

        return new Found(List.copyOf(found), nodes.size() + cases.size());
    }

    // UC-INTERNAL-001, Rule-INTERNAL-073
    public record Found(@NotNull List<Hit> hits, int matched) {
    }

    private static @NotNull List<DirectoryDto> everywhereToGo(final @NotNull ProjectIndexer indexer) {
        return indexer.getAllNodes().stream()
                .filter(DirectoryDto::isOpenableInEditor)
                .sorted(Hits::inTreeOrder)
                .toList();
    }

    private static @NotNull List<DirectoryDto> nodesNamed(final @NotNull ProjectIndexer indexer, final @NotNull String wanted) {
        return indexer.getAllNodes().stream()
                .filter(node -> contains(node.getName(), wanted))
                .sorted(Hits::byClosestName)
                .toList();
    }

    private static @NotNull List<TestCaseDto> casesMatching(final @NotNull ProjectIndexer indexer, final @NotNull String wanted) {
        if (tooShort(wanted)) return List.of();

        return indexer.getAllTestCases().stream()
                .filter(tc -> TestEditorAttributes.anyContains(tc, wanted))
                .toList();
    }

    private static @NotNull List<Hit> topCases(final @NotNull List<TestCaseDto> cases, final @NotNull String wanted, final int room) {
        if (room <= 0) return List.of();

        return cases.stream()
                .sorted(byDescriptionMatchThenText(wanted))
                .limit(room)
                .map(Hit::of)
                .toList();
    }

    // UC-INTERNAL-001, Rule-INTERNAL-001
    static boolean tooShort(final @NotNull String wanted) {
        return wanted.trim().length() < 2;
    }

    static int inTreeOrder(final @NotNull DirectoryDto one, final @NotNull DirectoryDto other) {
        final int byPlace = String.join(" > ", one.getPath2())
                .compareToIgnoreCase(String.join(" > ", other.getPath2()));

        return byPlace != 0 ? byPlace : one.getName().compareToIgnoreCase(other.getName());
    }

    static boolean contains(final @NotNull String value, final @NotNull String wanted) {
        return value.toLowerCase(Locale.ROOT).contains(wanted.toLowerCase(Locale.ROOT));
    }

    static int byClosestName(final @NotNull DirectoryDto one, final @NotNull DirectoryDto other) {
        final int byLength = Integer.compare(one.getName().length(), other.getName().length());

        return byLength != 0 ? byLength : one.getName().compareToIgnoreCase(other.getName());
    }

    static @NotNull Comparator<TestCaseDto> byDescriptionMatchThenText(final @NotNull String wanted) {
        return Comparator.comparingInt((TestCaseDto tc) -> contains(tc.getDescription(), wanted) ? 0 : 1)
                .thenComparing(TestCaseDto::getDescription, String.CASE_INSENSITIVE_ORDER);
    }
}
