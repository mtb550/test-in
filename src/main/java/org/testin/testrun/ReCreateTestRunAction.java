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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.creator.CreateTestRun;
import org.testin.explorer.tree.TreeValues;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.tree.TreePath;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReCreateTestRunAction extends DumbAwareAction {
    // UC-TREE-PANEL-021
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.tree(e)
                .map(SimpleTree::getSelectionPath)
                .ifPresent(path -> new Work(p).reCreateAt(path));
    }

    // UC-TREE-PANEL-021, Rule-TREE-PANEL-069
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelected(e, TestRunDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private record Work(@NotNull Project p) {
        private void reCreateAt(final @NotNull TreePath path) {
            TreeValues.directoryAt(path)
                    .filter(TestRunDirectoryDto.class::isInstance)
                    .map(TestRunDirectoryDto.class::cast)
                    .ifPresent(source -> TreeValues.directoryAt(path.getParentPath())
                            .ifPresent(parent -> reCreate(source, parent)));
        }

        private void reCreate(final @NotNull TestRunDirectoryDto source, final @NotNull DirectoryDto parent) {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull Set<UUID> testCases = indexer.getTestRunByPath(source.getPath()).coveredIds();

            final @NotNull Set<String> taken = indexer.getChildren(parent.getPath()).stream()
                    .map(DirectoryDto::getName)
                    .collect(Collectors.toSet());

            Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                    tp -> new CreateTestRun(p).configureRun(tp.getTestCasesDirectory(), NextRunName.after(source.getName(), taken), parent, testCases, source.getMarker().getConfiguration()),
                    () -> Logger.warn("Re-create test run: no test project is bound to " + p.getName()));
        }
    }
}
