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
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;

import java.nio.file.Path;
import java.util.Optional;

@AllArgsConstructor
public class CreateTestSet implements NodeCreator {
    private final @NotNull Project p;

    /**
     * UC-TREE-PANEL-007.
     * <p>
     * Makes the test set and answers with it, and generates nothing.
     * <p>
     * The class is the node type's generator's job, and it was being done
     * twice: the tree route creates the set and then runs that generator, so
     * every set created from the tree took the write lock twice for one
     * keystroke and logged "Test class already exists" about a class written a
     * moment earlier. It survived only because the second call finds the file
     * and returns it.
     * <p>
     * Two owners for one act is the problem rather than the wasted lock. A
     * change to how a test set's class is generated would have reached one
     * route and missed the other, with nothing failing to say so.
     */
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        final @NotNull TestSetDirectoryDto ts = Services.getInstance(p, DirectoryMapper.class).getTestSetNode(p, newDirPath, parentDir);

        Services.getInstance(p, ProjectIndexer.class).addTestSet(ts);

        return Optional.of(ts);
    }

}

