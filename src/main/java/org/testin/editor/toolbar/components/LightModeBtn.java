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

/**
 * Opens light mode, and says whether it is open (#13).
 * <p>
 * A toggle rather than an opener: pressed once it shows the window, pressed
 * again it closes it and the tester carries on in the editor. It stays pressed
 * for exactly as long as the window exists, so a glance at the toolbar answers
 * which mode you are in.
 * <p>
 * <b>Pressed is asked, not remembered.</b> {@link #updateState} asks whether
 * the window is showing <i>this</i> run, so the window closing by any route -
 * Escape, its own close button, another run taking it over, the project
 * shutting - un-presses this without anybody writing a third handler.
 * <p>
 * Enabled while the run is still open: a run that has been signed off has
 * nothing left to record, so there is nothing to open light mode for.
 */
public class LightModeBtn extends AbstractIconButton implements ToolbarItem {

    private final @NotNull Project p;
    private final @NotNull RunEditor editor;

    // UC-EDITOR-PANEL-046
    public LightModeBtn(final @NotNull RunEditor editor) {
        // https://intellij-icons.jetbrains.design/
        super(Bundle.message("toolbar.light.mode"), AllIcons.MeetNewUi.LightTheme);
        this.p = editor.getProject();
        this.editor = editor;

        addActionListener(e -> Services.getInstance(p, LightMode.class).toggle(editor, this::updateState));
    }

    /**
     * UC-EDITOR-PANEL-046, Rule-EDITOR-PANEL-008.
     * <p>
     * Gray says no; the tooltip says why. It was the only button on this toolbar
     * that grayed without a reason, so a tester on a completed run was left with
     * a button that had simply stopped working and nothing naming the status
     * that stopped it (#312, A30).
     * <p>
     * Shaped like Result Analysis's, which refuses for the same reason and
     * already names the status back.
     */
    public void updateState() {
        final boolean stillOpen = editor.getParent().isStillOpen();

        setEnabled(stillOpen);
        setToolTipText(stillOpen
                ? Bundle.message("toolbar.light.mode")
                : Bundle.message("toolbar.light.mode.disabled", editor.getParent().getMarker().getStatus().getLabel()));
        setOn(Services.getInstance(p, LightMode.class).isOpenOn(editor.getParent()));
    }
}
