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

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.DirectoryMapper;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestSetPackage implements NodeCreator {
    private final @NotNull Project p;

    // UC-TREE-PANEL-008, Rule-TREE-PANEL-027
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestSetPackageDirectoryDto tsp = Services.getInstance(p, DirectoryMapper.class).getTestSetPackageNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSetPackage(tsp);
        return Optional.of(tsp);
    }
}

