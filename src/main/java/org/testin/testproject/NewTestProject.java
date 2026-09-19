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

import lombok.AllArgsConstructor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.DirectoryMapper;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;

import java.nio.file.Path;
import java.util.Optional;

/**
 * UC-TREE-PANEL-002.
 * <p>
 * Makes a new, empty test project and binds this repository to it.
 * <p>
 * Not an action. It was one - an AnAction with a title, an icon, an update and
 * an actionPerformed - that nothing registered: the create dialog built it and
 * called execute, so the action half could never run (#312, A100).
 */
@AllArgsConstructor
public final class NewTestProject {
    private final @NotNull Project p;
    private final @NotNull TreePanel tp;
    private final @NotNull String tpName;

    // UC-TREE-PANEL-002, Rule-TREE-PANEL-017
    public void execute() {

        final @NotNull Path tpPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(tpName);

        if (Services.getInstance(p, ProjectIndexer.class).isTaken(tpPath, Optional.empty())) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, tpName);
            return;
        }

        final @NotNull TestProjectDirectoryDto created = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(p, tpPath);

        // A project whose markers did not land is not bound, drawn or confirmed:
        // the write has already said why (#312, A5).
        if (!Services.getInstance(p, ProjectIndexer.class).addTestProject(created)) return;

        // A repository asks for exactly one test project, so the one it just made
        // is the one it is about - chosen on this machine; creating one never
        // writes testin.yml (#8, Rule-TREE-PANEL-106).
        Services.getInstance(p, BoundTestProject.class).choose(created.getName());

        tp.refresh();
        Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);
    }

}
