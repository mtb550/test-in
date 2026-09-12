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
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;

import javax.swing.tree.TreePath;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The next cycle, from the one before it.
 * <p>
 * A test cycle is a test run, and cycle 2 usually covers what cycle 1 covered.
 * Rebuilding that by hand means re-ticking every case in the selection tree and
 * retyping every configuration answer - slow on a run of any size, and a missed
 * tick silently changes what the cycle was for (#9).
 * <p>
 * This opens the same dialog creating a run opens, holding the previous cycle's
 * cases and its configuration, with a name suggested. Nothing else is carried:
 * the run is written by the same path a new one is, which builds its items from
 * the ticked cases and gives it a fresh marker - so no verdict, duration or
 * stack trace from the last cycle can reach this one.
 * <p>
 * Declared in {@code plugin.xml} (#119), so the platform builds one instance for
 * the whole IDE and what it acts on comes from the keystroke. The work is in
 * {@link Work} for the same reason {@code JavaSourceRoot.RootWork} is separate:
 * an action is a gesture and an answer to "is this available", and everything
 * else it was carrying belongs to something that has a project to work with.
 */
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
        // update() reads the tree's selection, which is Swing state.
        return ActionUpdateThread.EDT;
    }

    /**
     * Re-creating one run, for a project that is there.
     */
    private record Work(@NotNull Project p) {

        /**
         * The run and the folder it sits in, read off the same path - the parent
         * is taken from the tree rather than from the node, which carries it as
         * a field that may not be set.
         */
        private void reCreateAt(final @NotNull TreePath path) {
            TreeValues.directoryAt(path)
                    .filter(TestRunDirectoryDto.class::isInstance)
                    .ifPresent(source -> TreeValues.directoryAt(path.getParentPath())
                            .ifPresent(parent -> reCreate(source, parent)));
        }

        private void reCreate(final @NotNull DirectoryDto source, final @NotNull DirectoryDto parent) {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            final @NotNull TestRunDto run = indexer.getTestRunByPath(source.getPath());
            final @NotNull Set<UUID> cases = run.getResults().stream().map(TestRunItems::getId).collect(Collectors.toSet());

            final @NotNull Set<String> taken = indexer.getChildren(parent.getPath()).stream()
                    .map(DirectoryDto::getName)
                    .collect(Collectors.toSet());

            // A case removed since the source run is simply not in the tree, so it
            // is not ticked and not carried. Nothing to report and nothing to skip:
            // the tree is built from what exists now.
            Services.getInstance(p, BoundTestProject.class).get().ifPresentOrElse(
                    tp -> new CreateTestRun(p).configureRun(tp.getTestCasesDirectory(), NextRunName.after(source.getName(), taken), parent, cases, run.getConfiguration()),
                    () -> Logger.warn("Re-create test run: no test project is bound to " + p.getName()));
        }
    }
}
