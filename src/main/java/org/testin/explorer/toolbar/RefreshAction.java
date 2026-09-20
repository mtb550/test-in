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

package org.testin.explorer.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.notifications.Notifier;
import org.testin.editor.TestinEditors;
import org.testin.services.Services;
import org.testin.notifications.Done;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RefreshAction extends AbstractProjectAction {
    private static final @NotNull String REFRESHED = Done.REFRESHED.getOutcome();

    private final @NotNull TreePanel tp;

    private final @NotNull AtomicBoolean refreshGuard = new AtomicBoolean(false);

    // UC-TREE-PANEL-025, UC-TREE-PANEL-026, Rule-TREE-PANEL-081
    private final @NotNull AtomicReference<Optional<String>> queued = new AtomicReference<>(Optional.empty());

    public RefreshAction(final @NotNull Project p, final @NotNull TreePanel tp) {
        super(p, Bundle.message("toolbar.refresh"), Bundle.message("toolbar.refresh.description"), AllIcons.Actions.Refresh);
        this.tp = tp;
    }

    // UC-TREE-PANEL-025
    public void execute() {
        execute(REFRESHED);
    }

    // UC-TREE-PANEL-025, UC-TREE-PANEL-026, Rule-TREE-PANEL-081
    public void execute(final @NotNull String outcome) {
        if (!refreshGuard.compareAndSet(false, true)) {
            queued.set(Optional.of(outcome));
            Logger.info("Refresh: already in progress, so this one waits for it - " + outcome);
            return;
        }

        Logger.info("Refresh: re-indexing started");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Services.getInstance(p, BoundTestProject.class).reread();

                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.resetForReindex();
                indexer.indexWithProgress();
                indexer.awaitIndexing();

                Logger.info("Refresh: re-indexing complete, rebuilding tree");

                ApplicationManager.getApplication().invokeLater(() -> rebuildTree(outcome));

            } catch (final Exception ex) {
                Logger.error("Refresh: re-indexing failed - " + ex.getMessage());
                releaseAndRunWhatWaited();
                Services.getInstance(p, Notifier.class).error(p, Bundle.message("toolbar.refresh.failed.title"), ex.getMessage());
            }
        });
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-081
    private void rebuildTree(final @NotNull String outcome) {
        try {
            if (p.isDisposed()) return;

            Services.getInstance(p, TestinEditors.class).refreshOpen(p);

            tp.refresh();
            Logger.info("Refresh: tree rebuilt");

            tp.fetchBranches();

            Services.getInstance(p, Notifier.class).softShow(p, outcome);

        } finally {
            releaseAndRunWhatWaited();
        }
    }

    // UC-TREE-PANEL-025, Rule-TREE-PANEL-081
    private void releaseAndRunWhatWaited() {
        refreshGuard.set(false);

        queued.getAndSet(Optional.empty()).ifPresent(this::execute);
    }

    // UC-TREE-PANEL-025
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        execute();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
