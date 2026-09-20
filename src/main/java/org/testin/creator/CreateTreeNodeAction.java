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

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.codegen.JavaCode;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.creator.dialogs.CreateRunDialog;
import org.testin.creator.dialogs.CreateTestDialog;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.DirectoryType;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.notifications.Refused;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.editor.TestinEditors;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

// UC-TREE-PANEL-007, UC-TREE-PANEL-009
public class CreateTreeNodeAction extends DumbAwareAction {
    private static final @NotNull String CREATES = Bundle.message("action.Testin.CreateNode.description");

    // UC-TREE-PANEL-007, UC-TREE-PANEL-009
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.singleSelectedNode(e).ifPresent(dir -> new Work(p).createUnder(dir));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-025
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<DirectoryDto> selected = TestinData.singleSelectedNode(e);
        final boolean enabled = selected.filter(DirectoryDto::canCreateChildren).isPresent();

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setDescription(whyNot(selected));

        e.getPresentation().setText(enabled
                ? Bundle.message("action.Testin.CreateNode.text")
                : Bundle.message("action.Testin.CreateNode.text.full", shortWhyNot(selected)));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-096
    private static @NotNull String shortWhyNot(final @NotNull Optional<DirectoryDto> selected) {
        return selected
                .map(dir -> Bundle.message("create.node.nothing.under", dir.getType().getMarkerKind()))
                .orElseGet(() -> Bundle.message("create.node.select.one"));
    }

    // UC-TREE-PANEL-007, Rule-TREE-PANEL-096
    private static @NotNull String whyNot(final @NotNull Optional<DirectoryDto> selected) {
        if (selected.isEmpty()) {
            return Bundle.message("create.node.why.select", DirectoryType.TCD.getMarkerKind(), DirectoryType.TRD.getMarkerKind(),
                    DirectoryType.TSP.getMarkerKind(), DirectoryType.TRP.getMarkerKind());
        }

        final @NotNull DirectoryDto dir = selected.orElseThrow();
        if (dir.canCreateChildren()) return CREATES;

        return Bundle.message("create.node.why.holds", dir.getType().getMarkerKind(), DirectoryType.TSP.getDescription(), DirectoryType.TRP.getDescription());
    }

    private record Work(@NotNull Project p) {
    // UC-TREE-PANEL-007, UC-TREE-PANEL-008, UC-TREE-PANEL-009, UC-TREE-PANEL-010, Rule-TREE-PANEL-004
    private void createUnder(final @NotNull DirectoryDto pDir) {
        final @NotNull BiConsumer<String, DirectoryType> onCreate = (s, dt) -> {
            if (s.isEmpty()) return;
            final @NotNull Path newDirPath = pDir.getPath().resolve(s);

            if (Services.getInstance(p, ProjectIndexer.class).nodeExists(newDirPath)) {
                Services.getInstance(p, Notifier.class).softRefuse(p, Refused.ALREADY_EXISTS, s);
                return;
            }

            final @NotNull Optional<DirectoryDto> created = NodeCreators.of(p, dt).execute(s, pDir, newDirPath);
            Services.getInstance(p, TreePanel.class).getProjectTree().refresh();

            created.ifPresent(dir -> {
                Services.getInstance(p, Notifier.class).softShow(p, Done.CREATED);

                if (dt == DirectoryType.TS)
                    Services.getInstance(p, TestinEditors.class).open(p, dir);

                JavaCode.of(dt).getCreated().execute(p, dir);
            });

        };

        final @NotNull List<DirectoryType> kinds = pDir.childKinds();
        if (kinds.equals(DirectoryType.UNDER_TEST_CASES)) new CreateTestDialog(p, onCreate).show();
        else if (kinds.equals(DirectoryType.UNDER_TEST_RUNS)) new CreateRunDialog(p, onCreate).show();
    }
    }
}
