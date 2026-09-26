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

package org.testin.testrun;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.DialogSize;
import org.testin.ui.framework.SelectionTree;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

public final class RunConfigurationDialog extends AbstractFrameworkDialog {
    private final @NotNull RunFormAction action;
    private final @NotNull RunConfigurationForm form;
    private final @NotNull SelectionTree selection;

    public RunConfigurationDialog(final @NotNull Project p, final @NotNull RunConfigurationForm form, final @NotNull SelectionTree selection, final @NotNull RunFormAction action) {
        super(p);
        this.action = action;
        this.form = form;
        this.selection = selection;

        title = action.title();

        final @NotNull ComponentDialogBase<DialogButton> confirm = ComponentDialogBase.button(action.button());
        components = List.of(
                ComponentDialogBase.of(form),
                ComponentDialogBase.of(selection),
                confirm);

        shortcuts = List.of(
                StatusBarShortcut.navigate(),
                StatusBarShortcut.hint("Space", Bundle.message("shortcut.check")),
                StatusBarShortcut.cancel(this::closeCancel));

        size = DialogSize.HALF;

        final @NotNull DialogButton confirmButton = confirm.getComponent();
        final @NotNull Runnable refresh = () -> confirmButton.enableUnless(whyNot());

        refresh.run();
        selection.onCheckChanged(refresh);
        form.onAnswerChanged(refresh);
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-029, Rule-TREE-PANEL-121
    private @NotNull Optional<String> whyNot() {
        if (!selection.hasChecked()) return Optional.of(Bundle.message("run.form.no.test.case"));

        return form.unanswered();
    }

    // UC-TREE-PANEL-009, Rule-TREE-PANEL-029, Rule-TREE-PANEL-121
    @Override
    protected void submit() {
        if (whyNot().isPresent()) return;

        if (action.submit().saved(form, selection)) closeOk();
    }
}
