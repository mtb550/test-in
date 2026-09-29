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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.TestinEditors;
import org.testin.indexer.DirectoryMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestRuns;
import org.testin.logger.Logger;
import org.testin.model.TestRunConfiguration;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.BackgroundWork;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.testrun.RunConfigurationForm;
import org.testin.testrun.RunForm;
import org.testin.testrun.RunFormAction;
import org.testin.ui.framework.SelectionTree;
import org.testin.util.Bundle;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CreateTestRun implements NodeCreator {
    private final @NotNull Project p;
    private final @NotNull BoundTestProject boundTestProject;
    private final @NotNull Nodes nodes;
    private final @NotNull Notifier notifier;
    private final @NotNull DirectoryMapper directoryMapper;
    private final @NotNull TestRuns testRuns;
    private final @NotNull TestinEditors editors;

    public CreateTestRun(final @NotNull Project p) {
        this.p = p;
        this.boundTestProject = Services.getInstance(p, BoundTestProject.class);
        this.nodes = Services.getInstance(p, Nodes.class);
        this.notifier = Services.getInstance(p, Notifier.class);
        this.directoryMapper = Services.getInstance(p, DirectoryMapper.class);
        this.testRuns = Services.getInstance(p, TestRuns.class);
        this.editors = Services.getInstance(p, TestinEditors.class);
    }

    // UC-TREE-PANEL-009
    @Override
    public @NotNull Optional<DirectoryDto> execute(final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Path newDirPath) {
        boundTestProject.get().ifPresentOrElse(
                tp -> configureRun(tp.getTestCasesDirectory(), name, parentDir, Set.of(), Map.of()),
                () -> Logger.warn("Create test run: no test project is bound to " + p.getName()));

        return Optional.empty();
    }

    // UC-TREE-PANEL-009, UC-TREE-PANEL-021
    public void configureRun(final @NotNull DirectoryDto testCasesRoot, final @NotNull String name, final @NotNull DirectoryDto parentDir, final @NotNull Set<UUID> sourceTestCases, final @NotNull Map<TestRunConfiguration, String> sourceConfiguration) {
        new RunForm(p).open(testCasesRoot, name, sourceTestCases, sourceConfiguration,
                new RunFormAction(Bundle.message("run.create.title"), Bundle.message("run.create.button"), (form, selection) -> create(form, selection, parentDir)));
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-004
    private boolean create(final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull DirectoryDto parentDir) {
        final @NotNull String name = form.getRunName();
        if (name.isEmpty()) {
            notifier.softRefuse(p, Bundle.message("run.needs.a.name"));
            return false;
        }

        if (!nodes.nodeExists(parentDir.getPath())) {
            notifier.softRefuse(p, Bundle.message("run.parent.gone", parentDir.getName()));
            return false;
        }

        final @NotNull Path savePath = parentDir.getPath().resolve(name);
        if (nodes.nodeExists(savePath)) {
            notifier.softRefuse(p, Refused.ALREADY_EXISTS, name);
            return false;
        }

        final @NotNull TestRunDirectoryDto runDir = directoryMapper.setTestRunNode(savePath, parentDir);
        write(form, selection, savePath, runDir);

        return true;
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-031
    private void write(final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull Path savePath, final @NotNull TestRunDirectoryDto trDir) {
        final @NotNull Map<TestRunConfiguration, String> configuration = form.configuration();

        final @NotNull TestRunDto tr = new TestRunDto().coverOnly(RunForm.checkedTestCases(selection));

        BackgroundWork.run(p, Bundle.message("run.task.creating", savePath.getFileName()), Bundle.message("run.create.failed.title"), _ -> {
            trDir.getMarker().configure(TestRunConfiguration.answered(configuration));

            if (!nodes.addTestRunDir(trDir)) return;

            testRuns.putTestRun(savePath, tr);
            nodes.refreshDirectory(savePath);

            ApplicationManager.getApplication().invokeLater(() -> {
                editors.open(p, trDir);

                notifier.softShow(p, Done.CREATED);
            });

        });
    }
}
