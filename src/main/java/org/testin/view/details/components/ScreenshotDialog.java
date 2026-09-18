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

package org.testin.view.details.components;

import com.intellij.util.ui.JBUI;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.Picture;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

/**
 * One screenshot pasted with a failure, at its real size, for looking at (#50).
 * <p>
 * Built like {@link ErrorDetailsDialog}, its neighbor in the Stacktrace row: the
 * text opens in that one, and each screenshot in this one. Nothing is saved.
 */
public final class ScreenshotDialog extends AbstractFrameworkDialog<Picture> {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-081
    public ScreenshotDialog(final @NotNull Project p, final @NotNull String name, final byte @NotNull [] png) {
        super(p);

        title = name;

        // Sized rather than left to the picture: a screenshot is as large as the
        // screen it was taken of. Setting it also makes the popup resizable.
        preferredSize = JBUI.size(WIDTH, HEIGHT);

        components = List.of(ComponentDialogBase.picture(png));

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));
    }

    @Override
    protected void submit() {
        closeOk();
    }

    /**
     * UC-INTERNAL-007, Rule-INTERNAL-075. Shows one screenshot. Asked for another, the newer one is the one wanted.
     */
    @Override
    protected boolean replacesItsKind() {
        return true;
    }
}
