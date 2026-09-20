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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.editor.CardHoverAction;

import javax.swing.JComponent;

// UC-VIEW-PANEL-012, UC-VIEW-PANEL-014, Rule-VIEW-PANEL-003
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ShownCaseAction extends DumbAwareAction {
    private final @NotNull ViewPanel panel;
    private final @NotNull CardHoverAction action;

    static void bind(final @NotNull ViewPanel panel, final @NotNull CardHoverAction action, final @NotNull JComponent component) {
        new ShownCaseAction(panel, action).registerCustomShortcutSet(Declared.shortcutSet(action.getActionId()), component);
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        panel.getCurrentTestCase().ifPresent(shown -> action.execute(panel.getP(), shown));
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(panel.getCurrentTestCase().isPresent() && action.enableOrExplain(e.getPresentation()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
