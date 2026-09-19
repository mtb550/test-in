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

import org.testin.actions.GrayWithReason;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.creator.dialogs.CreateProjectDialog;
import org.testin.explorer.TreePanel;
import org.testin.git.GitRefs;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.testproject.CloneTestProject;
import org.testin.testproject.NewTestProject;
import org.testin.services.OptionalPlugin;
import org.testin.util.Bundle;
import org.testin.indexer.ProjectIndexer;
import org.testin.notifications.Refused;

import java.nio.file.Path;
import java.util.Optional;

public class CreateTestProjectAction extends AbstractProjectAction {
    private final @NotNull TreePanel tp;

    public CreateTestProjectAction(final @NotNull Project p, final @NotNull TreePanel tp) {
        super(p, Bundle.message("project.new.text"), Bundle.message("project.new.description"), AllIcons.General.Add);
        this.tp = tp;
    }

    // UC-TREE-PANEL-002
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * UC-TREE-PANEL-002, UC-TREE-PANEL-003, Rule-TREE-PANEL-107.
     * <p>
     * Direct entry point for the tree panel's empty state — no AnActionEvent required.
     */
    public void execute() {

        new CreateProjectDialog(p, name -> {
            // What was typed decides: a repository URL is cloned, anything else
            // is a name for a new project.
            if (!GitRefs.isRepositoryUrl(name)) {
                new NewTestProject(p, tp, name).execute();
                return;
            }

            if (!OptionalPlugin.GIT.isAvailableOrWarn(p)) return;

            // Named after its repository, so a code project with no testin.yml
            // clones as readily as one with it (Rule-TREE-PANEL-107). Only the
            // name testin.yml gives with its own address is never numbered, so a
            // folder of that name already here is said rather than cloned over.
            final @NotNull String projectName = CloneTestProject.nameFor(p, name);
            final @NotNull Path folder = Services.getInstance(p, TestinRoot.class).getPath().resolve(projectName);

            if (Services.getInstance(p, ProjectIndexer.class).isTaken(folder, Optional.empty())) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, projectName);
                return;
            }

            new CloneTestProject(p, name, projectName, tp).execute();

        }).show();
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-115
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Both branches, the way Select Test Project answers the same question.
        // Only the false branch was written here, and a presentation keeps
        // whatever it was last told - so once this had been drawn without a
        // Testin folder it stayed gray for the rest of the IDE session, however
        // the setting changed afterwards (#189).
        // Gray with the reason, as every gray entry is (#301, F7); both
        // branches, so the reason goes when a folder is set.
        GrayWithReason.unless(this, e, Services.getInstance(p, TestinRoot.class).isConfigured(), Bundle.message("toolbar.disabled.no.root"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
