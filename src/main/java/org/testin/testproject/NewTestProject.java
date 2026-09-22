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

package org.testin.testproject;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.services.Services;

import java.nio.file.Path;

// UC-TREE-PANEL-002
@AllArgsConstructor
public final class NewTestProject {
    private final @NotNull Project p;
    private final @NotNull TreePanel tp;
    private final @NotNull String tpName;

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    public void execute() {
        TestProjectFolder.free(p, tpName).ifPresent(this::create);
    }

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    private void create(final @NotNull Path tpPath) {
        final @NotNull TestProjectDirectoryDto created = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(p, tpPath);

        if (!Services.getInstance(p, ProjectIndexer.class).addTestProject(created)) return;

        // Rule-TREE-PANEL-106
        Services.getInstance(p, BoundTestProject.class).choose(created.getName());

        tp.refresh();
        Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);
    }
}
