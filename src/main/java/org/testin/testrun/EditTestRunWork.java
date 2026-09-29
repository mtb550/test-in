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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.logger.Logger;
import org.testin.model.TestRunConfiguration;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.rename.NodeRename;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import javax.swing.tree.TreePath;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

record EditTestRunWork(@NotNull Project p, @NotNull TestRuns testRuns, @NotNull BoundTestProject boundTestProject, @NotNull Notifier notifier, @NotNull Nodes nodes, @NotNull TestinEditors editors) {
    EditTestRunWork(final @NotNull Project p) {
        this(p, Services.getInstance(p, TestRuns.class), Services.getInstance(p, BoundTestProject.class), Services.getInstance(p, Notifier.class), Services.getInstance(p, Nodes.class), Services.getInstance(p, TestinEditors.class));
    }

    void editAt(final @NotNull TreePath path) {
        EditTestRunAction.selectedRun(TreeValues.directoryAt(path)).ifPresent(this::edit);
    }

    private void edit(final @NotNull TestRunDirectoryDto run) {
        final @NotNull Set<UUID> covered = testRuns.getTestRunByPath(run.getPath()).coveredIds();

        boundTestProject.get().ifPresentOrElse(
                tp -> new RunForm(p).open(tp.getTestCasesDirectory(), run.getName(), covered, run.getMarker().getConfiguration(), saves(run)),
                () -> Logger.warn("Edit test run: no test project is bound to " + p.getName()));
    }

    private @NotNull RunFormAction saves(final @NotNull TestRunDirectoryDto run) {
        return new RunFormAction(Bundle.message("run.edit.title"), StatusBarShortcut.SAVE, (form, selection) -> save(run, form, selection));
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-074, Rule-TREE-PANEL-076
    private boolean save(final @NotNull TestRunDirectoryDto run, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection) {
        final @NotNull String name = form.getRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("run.needs.a.name"));
            return false;
        }

        if (!nodes.nodeExists(run.getPath())) {
            notifier.softRefuse(p, Bundle.message("run.gone", run.getName()));
            return false;
        }

        if (!run.isStillOpen()) {
            notifier.softRefuse(p, Bundle.message("run.status.changed", run.getName(), run.getMarker().getStatusLabel()));
            return false;
        }

        if (!name.equals(run.getName()) && NodeRename.refused(p, run, name)) return false;

        final @NotNull Set<UUID> checked = RunForm.checkedTestCases(selection);
        final @NotNull Set<UUID> offered = RunForm.offeredTestCases(selection);
        final @NotNull Map<TestRunConfiguration, String> configuration = TestRunConfiguration.answered(form.configuration());

        applyEdit(run, name, runPath -> {
            testRuns.changeRun(runPath, held -> held.cover(wanted(held, checked, offered::contains)));
            testRuns.changeRunMarker(runPath, marker -> marker.configure(configuration));
        }, () -> notifier.softShow(p, Done.UPDATED));

        return true;
    }

    // UC-TREE-PANEL-022, Rule-TREE-PANEL-076
    private @NotNull Set<UUID> wanted(final @NotNull TestRunDto from, final @NotNull Set<UUID> checked, final @NotNull Predicate<UUID> couldBeTicked) {
        final @NotNull Set<UUID> wanted = new LinkedHashSet<>(checked);
        from.coveredIds().stream()
                .filter(couldBeTicked.negate())
                .forEach(wanted::add);
        return wanted;
    }

    private void applyEdit(final @NotNull TestRunDirectoryDto run, final @NotNull String toName, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
        final @NotNull Path from = run.getPath();

        if (toName.equals(run.getName())) {
            write(from, writeTo, onDone);
            return;
        }

        NodeRename.apply(p, run, toName, () -> write(from.resolveSibling(toName), writeTo, onDone));
    }

    private void write(final @NotNull Path runPath, final @NotNull Consumer<Path> writeTo, final @NotNull Runnable onDone) {
        writeTo.accept(runPath);

        BackgroundWork.run(p, Bundle.message("run.task.updating", runPath.getFileName()), Bundle.message("run.update.failed.title"), _ -> {
            nodes.refreshDirectory(runPath);

            ApplicationManager.getApplication().invokeLater(() -> {
                editors.reloadOpen(runPath);

                onDone.run();
            });
        });
    }
}
