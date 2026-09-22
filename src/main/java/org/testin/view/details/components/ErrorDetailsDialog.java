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

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;

public final class ErrorDetailsDialog extends AbstractFrameworkDialog {
    private static final int VISIBLE_ROWS = 22;
    private static final int WIDTH = 900;
    private static final int HEIGHT = 600;

    // UC-VIEW-PANEL-006, Rule-VIEW-PANEL-036
    public ErrorDetailsDialog(final @NotNull Project p, final @NotNull String caseDescription, final @NotNull String message, final @NotNull String stacktrace) {
        super(p);

        title = Bundle.message("dialog.error.title");

        preferredSize = JBUI.size(WIDTH, HEIGHT);

        components = List.of(
                ComponentDialogBase.details()
                        .row(Bundle.message("caption.test.case"), caseDescription)
                        .build(),
                ComponentDialogBase.textArea()
                        .value(fullText(message, stacktrace))
                        .rows(VISIBLE_ROWS)
                        .build());

        shortcuts = List.of(StatusBarShortcut.build(Shortcuts.Escape, Bundle.message("shortcut.close"), this::closeCancel));
    }

    private static @NotNull String fullText(final @NotNull String message, final @NotNull String stacktrace) {
        if (message.isBlank()) return stacktrace;
        if (stacktrace.isBlank()) return message;

        return message + "\n\n" + stacktrace;
    }

    @Override
    protected void submit() {
        closeOk();
    }
}
