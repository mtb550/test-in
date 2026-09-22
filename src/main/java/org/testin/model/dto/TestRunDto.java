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

package org.testin.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString()
public class TestRunDto {
    @NotNull
    @Builder.Default
    private List<TestRunItems> results = new ArrayList<>();

    public @NotNull TestRunDto coverOnly(final @NotNull Set<UUID> wanted) {
        final @NotNull Map<UUID, TestRunItems> held = new LinkedHashMap<>();
        results.forEach(item -> held.put(item.getId(), item));

        final @NotNull List<TestRunItems> covered = new ArrayList<>();
        held.forEach((id, item) -> {
            if (wanted.contains(id)) covered.add(item);
        });

        wanted.stream()
                .filter(id -> !held.containsKey(id))
                .forEach(id -> covered.add(new TestRunItems().setId(id).setStatus(TestStatus.PENDING)));

        return new TestRunDto().setResults(covered);
    }

    @JsonIgnore
    public boolean isFullyJudged() {
        return !results.isEmpty() && results.stream().allMatch(TestRunItems::isJudged);
    }

    public @NotNull Set<UUID> coveredIds() {
        return results.stream().map(TestRunItems::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public @NotNull Optional<TestRunItems> resultOf(final @NotNull UUID testCaseId) {
        return results.stream().filter(item -> item.getId().equals(testCaseId)).findFirst();
    }
}