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

package org.testin.bug;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;
import org.testin.config.BugRepository;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.DialogButton;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextArea;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;

import java.util.List;
import java.util.Optional;

final class ReportBugDialog extends AbstractFrameworkDialog<TextInput> {
    private final @NotNull BugReports.RunItem item;
    private final @NotNull PreparedBug bug;
    private final @NotNull Runnable redraw;

    private final @NotNull ComponentDialogBase<TextInput> titleField;
    private final @NotNull ComponentDialogBase<TextArea> bodyArea;
    private final @NotNull ComponentDialogBase<DialogButton> sendButton;

    private boolean sent;

    ReportBugDialog(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull PreparedBug bug, final @NotNull Runnable redraw) {
        super(p);
        this.item = item;
        this.bug = bug;
        this.redraw = redraw;

        final @NotNull BugReports.Edits opening = Services.getInstance(p, BugReports.class).unsent(item)
                .orElse(new BugReports.Edits(bug.facts().title(), bug.body()));

        titleField = ComponentDialogBase.textField()
                .placeholder(Bundle.message("bug.dialog.title.placeholder"))
                .value(opening.title())
                .build();
        bodyArea = ComponentDialogBase.textArea()
                .placeholder(Bundle.message("bug.dialog.body.placeholder"))
                .value(opening.body())
                .rows(20)
                .build();
        sendButton = ComponentDialogBase.button(Bundle.message("bug.dialog.send"));

        title = Bundle.message("bug.dialog.title");
        components = List.of(
                titleField,
                bodyArea,
                ComponentDialogBase.details()
                        .row(Bundle.message("bug.dialog.screenshots"), String.valueOf(bug.facts().screenshots().size()))
                        .row(Bundle.message("bug.dialog.repository"), bug.repository().map(BugRepository::displayName).orElse(""))
                        .build(),
                sendButton);
        shortcuts = List.of(StatusBarShortcut.cancel(this::closeCancel));

        titleField.getComponent().onTextChanged(this::refreshSend);
        bodyArea.getComponent().onTextChanged(this::refreshSend);
        refreshSend();
    }

    void open() {
        if (!show()) {
            final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
            reports.discard(item);
            if (reports.end(item, BugReports.Stage.OPEN)) redraw.run();
            return;
        }

        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
                if (!event.isOk()) reports.discard(item);

                if (reports.end(item, BugReports.Stage.OPEN)) redraw.run();
            }
        });
    }

    private void refreshSend() {
        sendButton.getComponent().enableUnless(whyNot());
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-071
    private @NotNull Optional<String> whyNot() {
        return bug.whyNotReady().or(() -> BugLimits.whyNot(titleField.getComponent().getText(), bodyArea.getComponent().getText(),
                bug.facts().screenshots().size()));
    }

    private @NotNull Optional<String> noLongerFailed() {
        return item.stillFailed(Services.getInstance(p, ProjectIndexer.class)).isPresent()
                ? Optional.empty()
                : Optional.of(Bundle.message("bug.no.longer.failed"));
    }

    // UC-VIEW-PANEL-016, Rule-VIEW-PANEL-069, Rule-VIEW-PANEL-070
    @Override
    protected void submit() {
        if (sent) return;

        final @NotNull Optional<String> whyNot = whyNot().or(this::noLongerFailed);
        sendButton.getComponent().enableUnless(whyNot);
        if (whyNot.isPresent()) return;

        sent = true;
        final @NotNull BugReports.Edits edits = new BugReports.Edits(titleField.getComponent().getText().strip(), bodyArea.getComponent().getText());
        BugFiling.send(p, item, bug.repository().orElseThrow(), edits, bug.facts().screenshots(), redraw);
        closeOk();
    }
}
