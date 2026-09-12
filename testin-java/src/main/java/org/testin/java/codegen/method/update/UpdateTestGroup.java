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

package org.testin.java.codegen.method.update;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.GenAction;
import org.testin.model.dto.TestCaseDto;

import java.util.List;

public class UpdateTestGroup extends UpdateTestBase implements GenAction {

    // UC-CODEGEN-012, Rule-CODEGEN-045
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        if (!(obj instanceof TestCaseDto tc)) return;

        applyUpdate(p, tc, "Update Test Case Group", pm -> {
            writeGroups(p, pm, tc);
            reformat(p, pm);
        });
    }

    /**
     * A selection of cases as one command and one undo entry - see
     * {@link UpdateTestBase#applyToEach} for what that is worth and what it
     * cost before (#66, finding 56).
     */
    @Override
    public void executeAll(final @NotNull Project p, final @NotNull List<?> items) {
        applyToEach(p, items, "Update Test Case Group", (pm, tc) -> writeGroups(p, pm, tc));
    }
}
