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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.AbstractIconButton;
import org.testin.editor.run.RunEditor;
import org.testin.lightmode.LightMode;
import org.testin.services.Services;
import org.testin.util.Bundle;

public class LightModeBtn extends AbstractIconButton implements ToolbarItem {
    private final @NotNull Project p;
    private final @NotNull RunEditor editor;

    // UC-EDITOR-PANEL-046
    public LightModeBtn(final @NotNull RunEditor editor) {
        super(Bundle.message("toolbar.light.mode"), AllIcons.MeetNewUi.LightTheme);
        this.p = editor.getProject();
        this.editor = editor;

        addActionListener(_ -> Services.getInstance(p, LightMode.class).toggle(editor, this::updateState));
    }

    // UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-008
    public void updateState() {
        final boolean stillOpen = editor.getParent().isStillOpen();

        setEnabled(stillOpen);
        describe(stillOpen
                ? Bundle.message("toolbar.light.mode")
                : Bundle.message("toolbar.light.mode.disabled", editor.getParent().getMarker().getStatus().getLabel()));
        setOn(Services.getInstance(p, LightMode.class).isOpenOn(editor.getParent()));
    }
}
