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

package org.testin.codegen;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.TestCaseDto;
import org.testin.services.Services;
import org.testin.testcase.TestCaseOrder;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionPosition {
    // UC-CODEGEN-002, Rule-CODEGEN-014
    public static int of(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return TestCaseOrder.positionOf(setOf(p, tc), tc);
    }

    // UC-CODEGEN-011, Rule-CODEGEN-042
    public static @NotNull List<TestCaseDto> setOf(final @NotNull Project p, final @NotNull TestCaseDto tc) {
        return TestCaseOrder.ordered(Services.getInstance(p, ProjectIndexer.class).getTestCasesForTestSet(tc.getParent().getPath()));
    }
}
