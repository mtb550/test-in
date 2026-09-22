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

package org.testin.testrun;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.editor.run.RunEditor;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestStatus;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.model.markers.TestRunMarker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.nio.file.Path;
import java.util.Optional;

@Service(Service.Level.PROJECT)
@AllArgsConstructor
public final class TestRunStatusChange {
    private final @NotNull Project p;

    // UC-TREE-PANEL-020, Rule-EDITOR-PANEL-008, Rule-TREE-PANEL-091
    public void apply(final @NotNull TestRunDirectoryDto run, final @NotNull TestRunStatus newStatus) {
        final @NotNull Optional<RunEditor> open = Services.getInstance(p, TestinEditors.class).runEditorFor(p, run);

        Logger.trace("Test run status changed: " + run.getName() + " = " + newStatus.getLabel());

        if (newStatus == TestRunStatus.COMPLETED) open.ifPresent(RunEditor::stopExecution);

        run.getMarker().setStatus(newStatus);

        persist(run, open);
        redraw(open);

        Services.getInstance(p, Notifier.class).softShow(p, newStatus.getLabel());
    }

    private void persist(final @NotNull TestRunDirectoryDto run, final @NotNull Optional<RunEditor> open) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull TestRunStatus status = run.getMarker().getStatus();
        final @NotNull String tester = Services.getInstance(p, AppSettingsState.class).testerName;

        if (status.isTerminal()) finish(run.getPath());

        indexer.changeRunMarker(run.getPath(), marker -> {
            marker.setStatus(status);
            marker.touch(tester);
        });

        if (open.isPresent()) indexer.saveRun(run.getPath());
    }

    private void finish(final @NotNull Path runPath) {
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

        indexer.changeRun(runPath, run -> {
            int closed = 0;
            for (final TestRunItems item : run.getResults()) {
                if (item.shownStatus() == TestStatus.PENDING) {
                    item.setStatus(TestStatus.UNTESTED);
                    closed++;
                }
            }

            if (closed > 0)
                Logger.info("Run finished with " + closed + " case(s) not executed; marked untested: " + runPath);
        });

        indexer.changeRunMarker(runPath, TestRunMarker::markExecutionEnded);
    }

    // UC-TREE-PANEL-020, Rule-TREE-PANEL-091
    private void redraw(final @NotNull Optional<RunEditor> open) {
        ApplicationManager.getApplication().invokeLater(() -> open.ifPresent(RunEditor::refreshAfterRunStatusChanged));

        if (Services.isNotCreated(p, TreePanel.class)) return;
        Services.getInstance(p, TreePanel.class).getProjectTree().refresh();
    }
}
