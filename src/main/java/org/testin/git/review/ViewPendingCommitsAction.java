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

package org.testin.git.review;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractAnyProjectAction;
import org.testin.actions.TestinData;
import org.testin.explorer.tree.TreeValues;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.OptionalPlugin;

import java.nio.file.Path;

public class ViewPendingCommitsAction extends AbstractAnyProjectAction {
    // UC-SHARE-009, Rule-SHARE-042
    public static void reviewFor(final @NotNull Project p, final @NotNull Path path) {
        new ViewPendingCommitsWork(p).openFor(path);
    }

    // UC-SHARE-010
    @Override
    protected void perform(final @NotNull AnActionEvent e, final @NotNull Project p) {
        TestinData.tree(e).flatMap(TreeValues::projectPath).ifPresent(path -> reviewFor(p, path));
    }

    // UC-SHARE-010
    @Override
    protected void update(final @NotNull AnActionEvent e, final @NotNull Project p) {
        // Rule-SHARE-105
        if (OptionalPlugin.GIT.grayedWithReason(this, e.getPresentation())) return;

        e.getPresentation().setEnabled(TestinData.firstSelected(e, TestProjectDirectoryDto.class).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
