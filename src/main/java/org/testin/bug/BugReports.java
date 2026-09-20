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

package org.testin.bug;

import com.intellij.openapi.components.Service;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service(Service.Level.PROJECT)
public final class BugReports {
    @Getter
    @AllArgsConstructor
    enum Stage {
        PREPARING(
                Bundle.message("bug.preparing")
        ),

        OPEN(
                Bundle.message("bug.open")
        ),

        SENDING(
                Bundle.message("bug.sending")
        );

        private final @NotNull String reason;
    }

    public record RunItem(@NotNull Path run, @NotNull UUID id) {
        public @NotNull Optional<TestRunItems> in(final @NotNull TestRunDto testRun) {
            return testRun.resultOf(id).filter(result -> !result.isRemoved());
        }

        public @NotNull Optional<TestRunItems> failedIn(final @NotNull TestRunDto testRun) {
            return in(testRun).filter(result -> result.getStatus() == TestStatus.FAILED);
        }

        public @NotNull Optional<TestRunItems> stillFailed(final @NotNull ProjectIndexer indexer) {
            return indexer.findTestRun(run).flatMap(this::failedIn);
        }
    }

    public record Edits(@NotNull String title, @NotNull String body) {
    }

    private final @NotNull Map<RunItem, Stage> onTheWay = new HashMap<>();
    private final @NotNull Map<RunItem, Edits> unsent = new HashMap<>();

    void begin(final @NotNull RunItem item) {
        onTheWay.put(item, Stage.PREPARING);
    }

    void moveTo(final @NotNull RunItem item, final @NotNull Stage stage) {
        onTheWay.replace(item, stage);
    }

    boolean end(final @NotNull RunItem item, final @NotNull Stage stage) {
        return onTheWay.remove(item, stage);
    }

    boolean anotherIsOpen(final @NotNull RunItem item) {
        return onTheWay.entrySet().stream().anyMatch(entry -> entry.getValue() == Stage.OPEN && !entry.getKey().equals(item));
    }

    void keep(final @NotNull RunItem item, final @NotNull Edits edits) {
        unsent.put(item, edits);
    }

    @NotNull Optional<Edits> unsent(final @NotNull RunItem item) {
        return Optional.ofNullable(unsent.get(item));
    }

    void discard(final @NotNull RunItem item) {
        unsent.remove(item);
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-072
    public @NotNull Optional<String> whyReportBugIsOff(final @NotNull RunItem item, final @NotNull TestRunItems result) {
        final @NotNull Optional<Stage> stage = Optional.ofNullable(onTheWay.get(item));
        if (stage.isPresent()) return stage.map(Stage::getReason);

        if (result.bugIssue().isPresent()) return Optional.of(Bundle.message("bug.already.reported"));
        if (anotherIsOpen(item)) return Optional.of(Bundle.message("bug.finish.open.report"));

        return Optional.empty();
    }
}
