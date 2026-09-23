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

package org.testin.explorer.tree;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.GrayWithReason;
import org.testin.actions.TestinData;
import org.testin.explorer.TreePanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.NodeStatus;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.markers.Marker;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;
import org.testin.util.Bundle;

import java.time.ZonedDateTime;
import java.util.Optional;

public class UpdateStatusAction extends DumbAwareAction {
    private final @NotNull NodeStatus status;

    public UpdateStatusAction(final @NotNull NodeStatus status) {
        super(status.getButtonName(), status.getButtonDescription(), AllIcons.Actions.Edit);
        this.status = status;
    }

    // UC-TREE-PANEL-018, UC-TREE-PANEL-019
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        selected(e).ifPresent(dir -> mark(p, dir));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-062
    private void mark(final @NotNull Project p, final @NotNull DirectoryDto dir) {
        final @NotNull Marker marker = dir.getMarker();
        final @NotNull NodeStatus before = marker.status();

        final @NotNull String modifiedByBefore = marker.getModifiedBy();
        final @NotNull ZonedDateTime modifiedAtBefore = marker.getModifiedAt();

        try {
            marker.applyStatus(status);
            marker.touch(Services.getInstance(p, AppSettingsState.class).testerName);

            if (!Services.getInstance(p, ProjectIndexer.class).persistMarker(dir)) {
                marker.applyStatus(before);
                marker.setModifiedBy(modifiedByBefore);
                marker.setModifiedAt(modifiedAtBefore);
                return;
            }

            Services.getInstance(p, TreePanel.class).getProjectTree().updateNodes();

            Services.getInstance(p, Notifier.class).softShow(p, status.getLabel());

        } catch (final Exception ex) {
            Logger.error("Unable to mark '" + dir.getName() + "' " + status.getLabel() + ": " + ex.getMessage());
            Services.getInstance(p, Notifier.class).error(p, Bundle.message("status.failed", status.getLabel()));
        }
    }

    private @NotNull Optional<DirectoryDto> selected(final @NotNull AnActionEvent e) {
        return TestinData.singleSelectedNode(e).filter(dir -> dir.getMarker().statuses().contains(status));
    }

    // UC-TREE-PANEL-018, Rule-TREE-PANEL-065
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull Optional<DirectoryDto> dir = selected(e);

        e.getPresentation().setVisible(dir.isPresent());
        GrayWithReason.unless(this, e, dir.filter(node -> node.getMarker().status() != status).isPresent(),
                Bundle.message("status.already.description", status.getLabel()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
