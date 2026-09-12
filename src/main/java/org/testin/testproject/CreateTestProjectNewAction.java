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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.indexer.DirectoryMapper;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.nio.file.Path;

public class CreateTestProjectNewAction extends AbstractProjectAction {
    private final @NotNull TreePanel tp;
    private final @NotNull String tpName;

    public CreateTestProjectNewAction(final @NotNull Project p, final @NotNull TreePanel tp, final @NotNull String name) {
        super(p, Bundle.message("project.new.text"), Bundle.message("project.new.only.description"), AllIcons.General.Add);
        this.tp = tp;
        this.tpName = name;
    }

    // UC-TREE-PANEL-002
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    /**
     * UC-TREE-PANEL-002, Rule-TREE-PANEL-017.
     * <p>
     * Direct entry point for dialog callbacks — no AnActionEvent required.
     */
    public void execute() {

        final @NotNull Path tpPath = Services.getInstance(p, TestinRoot.class).getPath().resolve(tpName);

        if (Services.getInstance(p, ProjectIndexer.class).projectExists(tpPath)) {
            Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, tpName);
            return;
        }

        final @NotNull TestProjectDirectoryDto created = Services.getInstance(p, DirectoryMapper.class).setTestProjectNode(p, tpPath);

        Services.getInstance(p, ProjectIndexer.class).addTestProject(created);

        // A repository asks for exactly one test project, so the one it just made
        // is the one it is about. Writing it here is what makes the next clone of
        // this repository open on it without being asked (#8).
        Services.getInstance(p, BoundTestProject.class).bind(created.getName());

        tp.refresh();
        Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);
    }


    // UC-TREE-PANEL-028, Rule-TREE-PANEL-089
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // Both branches, otherwise the action stays disabled for the whole session
        // once seen without a configured Testin root.
        e.getPresentation().setEnabled(Services.getInstance(p, TestinRoot.class).isConfigured());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
