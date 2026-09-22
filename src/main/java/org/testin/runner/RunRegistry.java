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

package org.testin.runner;

import org.jetbrains.annotations.NotNull;
import org.testin.model.RunStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class RunRegistry {
    private final @NotNull Set<String> launchedNames = ConcurrentHashMap.newKeySet();

    private final @NotNull Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, String> configOf = new ConcurrentHashMap<>();

    private final @NotNull Set<UUID> stopped = ConcurrentHashMap.newKeySet();

    private final @NotNull Map<UUID, RunStatus> verdict = new ConcurrentHashMap<>();

    void starting(final @NotNull UUID id) {
        pending.add(id);
        stopped.remove(id);
    }

    boolean take(final @NotNull UUID id) {
        return pending.remove(id);
    }

    void launched(final @NotNull Collection<UUID> ids, final @NotNull String runName) {
        ids.forEach(id -> configOf.put(id, runName));
        launchedNames.add(runName);
    }

    boolean launchedHere(final @NotNull String runName) {
        return launchedNames.contains(runName);
    }

    // UC-CODEGEN-008, Rule-CODEGEN-076
    @NotNull String freeName(final @NotNull String wanted) {
        if (!launchedNames.contains(wanted)) return wanted;

        return IntStream.iterate(2, next -> next + 1)
                .mapToObj(next -> wanted + " (" + next + ")")
                .filter(candidate -> !launchedNames.contains(candidate))
                .findFirst()
                .orElseThrow();
    }

    void notStarting(final @NotNull UUID id) {
        pending.remove(id);
        configOf.remove(id);
    }

    boolean isStopped(final @NotNull UUID id) {
        return stopped.contains(id);
    }

    boolean isRunning(final @NotNull UUID id) {
        return pending.contains(id) || configOf.containsKey(id);
    }

    @NotNull RunStatus statusOf(final @NotNull UUID id) {
        return isRunning(id) ? RunStatus.RUNNING : verdict.getOrDefault(id, RunStatus.IDLE);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-038
    void reported(final @NotNull UUID id, final @NotNull RunStatus status) {
        if (status == RunStatus.RUNNING) return;

        pending.remove(id);
        configOf.remove(id);

        if (status == RunStatus.IDLE && !stopped.contains(id)) return;

        verdict.put(id, status);
    }

    // UC-CODEGEN-009, Rule-CODEGEN-037
    @NotNull Stop stopping(final @NotNull List<UUID> asked) {
        final @NotNull List<UUID> running = asked.stream().filter(this::isRunning).toList();
        if (running.isEmpty()) return Stop.NOTHING;

        final @NotNull Set<String> runs = running.stream()
                .map(id -> configOf.getOrDefault(id, ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        final @NotNull List<UUID> testCases = Stream.concat(
                        running.stream(),
                        configOf.entrySet().stream().filter(e -> runs.contains(e.getValue())).map(Map.Entry::getKey))
                .distinct()
                .toList();

        testCases.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
            stopped.add(id);
        });

        return new Stop(runs, testCases);
    }

    @NotNull List<UUID> ended(final @NotNull String runName) {
        if (!launchedNames.remove(runName)) return List.of();

        final @NotNull List<UUID> abandoned = configOf.entrySet().stream()
                .filter(e -> e.getValue().equals(runName))
                .map(Map.Entry::getKey)
                .toList();

        abandoned.forEach(id -> {
            pending.remove(id);
            configOf.remove(id);
        });

        return abandoned;
    }

    record Stop(@NotNull Set<String> runs, @NotNull List<UUID> testCases) {
        static final @NotNull Stop NOTHING = new Stop(Set.of(), List.of());
    }
}
