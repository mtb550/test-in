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

package org.testin.clipboard;

import org.testin.notifications.Done;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.TestinData;
import org.testin.logger.Logger;
import org.testin.model.dto.TestCaseDto;
import org.testin.notifications.Notifier;
import org.testin.services.Services;
import org.testin.util.Mapper;

import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Declared in {@code plugin.xml} (#119) with CTRL+SHIFT+C, which the grid does
 * not claim - so unlike the plain CTRL+C beside it, this key can live in the
 * keymap and be rebound. The pair is the rule the plugin already holds: the
 * plain key acts on the content in front of you, CTRL+SHIFT acts on the node.
 */
public class CopyTestCaseNodeAction extends DumbAwareAction {

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
        final @Nullable Project p = e.getProject();
        if (p == null) return;

        final @NotNull List<TestCaseDto> tcs = TestinData.selectedCases(e);

        if (!tcs.isEmpty()) {
            try {
                Services.getInstance(p, CutState.class).clear();

                String json = Services.getInstance(p, Mapper.class).writeValueAsString(tcs);
                CopyPasteManager.getInstance().setContents(new StringSelection(json));

                Services.getInstance(p, Notifier.class).softShowCounted(p, Done.COPIED, tcs.size());

            } catch (final Exception ex) {
                Logger.error("Exception: " + ex.getMessage());
            }
        }
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(!TestinData.selectedCases(e).isEmpty());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
