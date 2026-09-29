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

import org.jetbrains.annotations.NotNull;
import org.testin.indexer.TestRuns;
import org.testin.model.TestRunItems;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestRunDto;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public record RunItem(@NotNull Path run, @NotNull UUID id) {
    public @NotNull Optional<TestRunItems> in(final @NotNull TestRunDto testRun) {
        return testRun.resultOf(id).filter(result -> !result.isRemoved());
    }

    public @NotNull Optional<TestRunItems> failedIn(final @NotNull TestRunDto testRun) {
        return in(testRun).filter(result -> result.getStatus() == TestStatus.FAILED);
    }

    public @NotNull Optional<TestRunItems> stillFailed(final @NotNull TestRuns testRuns) {
        return testRuns.findTestRun(run).flatMap(this::failedIn);
    }
}
