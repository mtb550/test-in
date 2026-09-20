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

package org.testin.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.content.ContentManager;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.AbstractProjectAction;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import javax.swing.*;
import java.util.Optional;
import java.util.function.Function;

// UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080
public final class ViewTabAction extends AbstractProjectAction {
    @AllArgsConstructor
    enum Direction {
        NEXT(
                Shortcuts.TabNext,
                Bundle.message("view.tab.next"),
                Bundle.message("view.tab.next.description"),
                AllIcons.Actions.Forward,
                ContentManager::selectNextContent
        ),

        PREVIOUS(
                Shortcuts.TabPrevious,
                Bundle.message("view.tab.previous"),
                Bundle.message("view.tab.previous.description"),
                AllIcons.Actions.Back,
                ContentManager::selectPreviousContent
        );

        private final @NotNull Shortcuts key;
        private final @NotNull String text;
        private final @NotNull String description;
        private final @NotNull Icon icon;
        private final @NotNull Function<ContentManager, ActionCallback> select;
    }

    private final @NotNull Direction direction;

    ViewTabAction(final @NotNull Project p, final @NotNull JComponent tab, final @NotNull Direction direction) {
        super(p, direction.text, direction.description, direction.icon);
        this.direction = direction;
        registerCustomShortcutSet(direction.key.getCustomShortcut(), tab);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        ViewToolWindowFactory.toolWindow(p).ifPresent(tw -> {
            final @NotNull ContentManager contents = tw.getContentManager();
            direction.select.apply(contents).doWhenDone(() -> Optional.ofNullable(contents.getSelectedContent())
                    .ifPresent(front -> contents.setSelectedContent(front, true)));
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
