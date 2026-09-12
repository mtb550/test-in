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
import org.testin.model.dto.TestRunDto;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A bug one test case has recorded, and the run it was recorded in.
 * <p>
 * A bug in Testin is not a thing of its own and is not a field of a test case.
 * It is what a run row says about a failure - how bad it is and how soon it must
 * be fixed - so a test case's bugs are found by looking through the runs it has
 * been in, and the same case can carry a different one in every cycle.
 * <p>
 * That is why this pairs the row with the run rather than holding the fields: a
 * tester reading "Blocker" wants to know which cycle found it, and the row and
 * the run name are two halves of one sentence.
 *
 * @param runPath where the run sits, which is also where its name comes from -
 *                a run does not carry one of its own
 * @param item    the run's row for this test case, which holds the verdict, the
 *                severity, the priority and what actually happened
 */
public record OpenBug(@NotNull Path runPath, @NotNull TestRunItems item) {

    /**
     * UC-VIEW-PANEL-008, Rule-VIEW-PANEL-064.
     * <p>
     * Every bug this test case has, newest run first.
     * <p>
     * Read from the runs the indexer already holds, so this costs a walk over
     * what is in memory rather than a read of anything. A case that has never
     * failed answers with an empty list, which is the ordinary case and is not a
     * state anybody has to check for.
     */
    public static @NotNull List<OpenBug> of(final @NotNull Map<Path, TestRunDto> runs, final @NotNull UUID caseId) {
        return runs.entrySet().stream()
                .flatMap(run -> run.getValue().getResults().stream()
                        .filter(item -> item.getId().equals(caseId))
                        .filter(FailureDetail::recordsABug)
                        .map(item -> new OpenBug(run.getKey(), item)))
                .sorted(Comparator.comparing((OpenBug bug) -> bug.item().getExecutedAt()).reversed())
                .toList();
    }

    /**
     * The run's name, which is its folder's.
     */
    public @NotNull String runName() {
        final @NotNull Path name = runPath.getFileName();

        return name == null ? runPath.toString() : name.toString();
    }
}
