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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.ProjectStatus;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.util.Bundle;

import java.util.Map;

/**
 * Changes which test project this repository is bound to (#8).
 * <p>
 * The way back. A repository names one project and the panel shows only that
 * one, which is the point - but a tester who bound the wrong one, or whose
 * project was renamed, would otherwise have to edit {@code testin.yml} by hand
 * to say so. One toolbar button, not a dropdown: choosing is a thing done twice
 * in a repository's life, not on every glance at the tree.
 */
public final class SelectTestProjectAction extends AbstractProjectAction {

    private final @NotNull TreePanel tp;

    public SelectTestProjectAction(final @NotNull Project p, final @NotNull TreePanel tp) {
        super(p, Bundle.message("project.select.text"), Bundle.message("project.select.description"), AllIcons.Actions.ModuleDirectory);
        this.tp = tp;
    }

    // UC-TREE-PANEL-004
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        // The listing walks the Testin root and reads a marker per project, so it
        // happens off the EDT; the dialog it feeds opens back on it (#66).
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull Map<String, ProjectStatus> underRoot = Services.getInstance(p, ProjectIndexer.class).testProjects();

            ApplicationManager.getApplication().invokeLater(() -> {
                if (p.isDisposed()) return;

                // An empty picker would say nothing at all. The button beside
                // this one is the answer, so the message points at it rather
                // than opening a dialog with no rows in it.
                if (underRoot.isEmpty()) {
                    Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("project.none.title"),
                            Bundle.message("project.none.message"));
                    return;
                }

                new BindTestProjectDialog(p, underRoot, tp::reindex).show();
            });
        });
    }

    // UC-TREE-PANEL-028, Rule-TREE-PANEL-089
    @Override
    public void update(final @NotNull AnActionEvent e) {
        // There is nothing to choose from without a root. Both branches, so the
        // button comes back once one is configured.
        e.getPresentation().setEnabled(Services.getInstance(p, TestinRoot.class).isConfigured());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads a setting, never Swing state (#52).
        return ActionUpdateThread.BGT;
    }
}
