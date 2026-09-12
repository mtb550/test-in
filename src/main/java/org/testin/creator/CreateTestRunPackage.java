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
import org.testin.model.dto.dirs.TestRunPackageDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestRunPackage implements NodeCreator {
    private final @NotNull Project p;

    // UC-TREE-PANEL-010, Rule-TREE-PANEL-033
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        TestRunPackageDirectoryDto tr = Services.getInstance(p, DirectoryMapper.class).getTestRunPackageNode(p, newDirPath, parentDir);

        // The indexer owns all file/dir I/O: it creates the directory + .trp marker
        // (with JSON content) and registers the node.
        Services.getInstance(p, ProjectIndexer.class).addTestRunPackage(tr);

        return Optional.of(tr);
    }
}

