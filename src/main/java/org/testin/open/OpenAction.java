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

package org.testin.open;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.services.Services;
import org.testin.editor.TestinEditors;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import java.util.List;

// UC-TREE-PANEL-005, UC-TREE-PANEL-006
public class OpenAction extends DumbAwareAction {
    // UC-TREE-PANEL-005, UC-TREE-PANEL-006, Rule-TREE-PANEL-022
    public static void execute(final @NotNull Project p, final @NotNull List<DirectoryDto> selected) {
        selected.stream()
                .filter(DirectoryDto::isOpenableInEditor)
                .forEach(dir -> {
                    Logger.info("open: " + dir.getPath());
                    Services.getInstance(p, TestinEditors.class).open(p, dir);
                });
    }

    // UC-TREE-PANEL-005, UC-TREE-PANEL-006
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        execute(p, TestinData.selectedNodes(e));
    }

    // UC-TREE-PANEL-005, Rule-TREE-PANEL-022
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.selectedNodes(e).stream()
                .anyMatch(DirectoryDto::isOpenableInEditor));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
