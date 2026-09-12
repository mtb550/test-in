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

package org.testin.editor.toolbar.components;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareToggleAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

/**
 * One check-mark entry in the toolbar filter popup: toggles a value in its
 * selection set and notifies the owning button.
 */
final class ToggleFilterAction<T> extends DumbAwareToggleAction {

    private final @NotNull T value;
    private final @NotNull Set<T> selection;
    private final @NotNull FilterMembership<T> membership;
    private final @NotNull Runnable onChanged;

    /**
     * @param icon the swatch a priority row shows, and null on every other row.
     *             Really null rather than an empty icon: a toggle draws its own
     *             checkmark where the icon would go, and an icon that is there
     *             but paints nothing takes that place (#71)
     */
    ToggleFilterAction(final @NotNull String text, final @Nullable Icon icon, final @NotNull T value, final @NotNull Set<T> selection, final @NotNull FilterMembership<T> membership, final @NotNull Runnable onChanged) {
        super(text, null, icon);
        this.value = value;
        this.selection = selection;
        this.membership = membership;
        this.onChanged = onChanged;
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent e) {
        return selection.contains(value);
    }

    // UC-EDITOR-PANEL-020
    @Override
    public void setSelected(final @NotNull AnActionEvent e, final boolean state) {
        membership.apply(value, selection, state);
        onChanged.run();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // BGT on purpose - update() reads only fields/services, never Swing state; do not switch to EDT (#52).
        return ActionUpdateThread.BGT;
    }
}
