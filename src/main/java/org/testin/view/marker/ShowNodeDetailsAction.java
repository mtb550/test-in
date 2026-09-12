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

package org.testin.view.marker;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.view.marker.MarkerDetailsViewDialog;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;


/**
 * UC-TREE-PANEL-027.
 * <p>
 * Declared in {@code plugin.xml} (#119): the platform builds one instance for
 * the IDE, so what it acts on comes from the keystroke rather than from whoever
 * built it, and its name, description and icon are in the XML the Keymap reads.
 */
public class ShowNodeDetailsAction extends DumbAwareAction {

    // UC-TREE-PANEL-027, Rule-TREE-PANEL-087
    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        TestinData.singleSelectedNode(e).ifPresent(dir -> new MarkerDetailsViewDialog(p, dir).show());
    }

    /**
     * UC-TREE-PANEL-027, Rule-TREE-PANEL-087.
     * <p>
     * The dialog is about one node, so several selected grays the entry. It had
     * no update() at all: the entry stayed black over any selection and opened
     * the first row's details, with nothing to say the rest had been passed over
     * (#192).
     */
    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(TestinData.singleSelectedNode(e).isPresent());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // EDT now: update() reads the tree's selection, which is Swing state (#52).
        return ActionUpdateThread.EDT;
    }

}
