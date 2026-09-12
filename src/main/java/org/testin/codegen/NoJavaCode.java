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
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

/**
 * The generator of the node types that produce no Java: the two fixed root
 * containers, test run packages and test runs. Test cases carry the automation,
 * and a run only records what was executed.
 * <p>
 * A class rather than a null on {@link org.testin.model.DirectoryType}, so
 * "generates nothing" is stated by the type instead of every caller having to
 * ask whether there is a generator at all.
 */
@AllArgsConstructor
public final class NoJavaCode implements GenAction {

    private final @NotNull String nodeType;

    // UC-CODEGEN-004
    @Override
    public void execute(final @NotNull Project p, final @NotNull Object obj) {
        Logger.debug(nodeType + " generates no Java code");
    }
}

