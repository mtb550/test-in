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

package org.testin.undo;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;

public class UndoAction extends AbstractProjectAction {
    private final @NotNull UndoDirection direction;
    private final @NotNull UndoScope scope;

    public UndoAction(final @NotNull Project p, final @NotNull JComponent on, final @NotNull UndoScope scope, final @NotNull UndoDirection direction) {
        super(p, direction.getTitle(), Bundle.message("undo.action.description", direction.getTitle()), direction.getIcon());
        this.direction = direction;
        this.scope = scope;
        this.registerCustomShortcutSet(Shortcuts.customShortcut(direction.getShortcut()), on);
    }

    // UC-TREE-PANEL-016, UC-EDITOR-PANEL-012
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @NotNull UndoHistories service = Services.getInstance(p, UndoHistories.class);

        if (!direction.can(service, scope)) return;

        if (direction.apply(service, scope)) Services.getInstance(p, Notifier.class).softShow(p, direction.getDone());
    }

    // UC-EDITOR-PANEL-012, Rule-EDITOR-PANEL-067
    @Override
    public void update(final @NotNull AnActionEvent e) {
        final @NotNull UndoHistories service = Services.getInstance(p, UndoHistories.class);

        e.getPresentation().setEnabled(direction.can(service, scope));
        e.getPresentation().setText((direction.getTitle() + " " + direction.next(service, scope)).trim());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
