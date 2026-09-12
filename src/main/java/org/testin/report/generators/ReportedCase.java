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

package org.testin.report.generators;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.Map;
import java.util.UUID;

/**
 * The test case a run item points at.
 * <p>
 * A run outlives the cases it ran. One can be deleted afterwards, or belong to
 * a test set this machine never pulled, and its id then finds nothing in the
 * details map the report was handed.
 * <p>
 * Shared because all four formats tested for that themselves and each printed
 * something different: Excel wrote "N/A", HTML wrote nothing, PDF and Word wrote
 * an em dash. They ask here now and get a case whose fields are empty rather
 * than no case at all, so what a format prints for a blank description is a
 * display decision it makes once, in one place, for both reasons a description
 * can be blank.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ReportedCase {

    static @NotNull TestCaseDto of(final @NotNull Map<UUID, TestCaseDto> detailsMap, final @NotNull UUID id) {
        return detailsMap.getOrDefault(id, new TestCaseDto());
    }
}
