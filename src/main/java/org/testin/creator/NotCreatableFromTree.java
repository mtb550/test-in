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

package org.testin.creator;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The creator of the node types the tree cannot create under a selection.
 * <p>
 * Two different reasons, both ending here: the Test Cases and Test Runs
 * containers are made with their project and never by the tester, and a test
 * project has no parent node — it is created at the Testin root from the panel,
 * by name or by cloning a URL, which {@link NodeCreator#execute} has no
 * argument for.
 */
@AllArgsConstructor
public final class NotCreatableFromTree implements NodeCreator {

    private final @NotNull String nodeType;

    // Rule-TREE-PANEL-002
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        Logger.info(nodeType + " is not created from the tree");
        return Optional.empty();
    }
}

