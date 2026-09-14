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

/**
 * The bug report, open for the tester to edit and send (#28).
 * <p>
 * Only a click on Send sends. There is no Enter key: Enter in the title does
 * nothing and Enter in the body starts a new line, because filing cannot be
 * taken back. Escape cancels, and asks first when anything was edited.
 */
final class ReportBugDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull BugReports.RunItem item;
    private final @NotNull PreparedBug bug;
    private final @NotNull Runnable redraw;

    private final @NotNull ComponentDialogBase<TextInput> titleField;
    private final @NotNull ComponentDialogBase<TextArea> bodyArea;
    private final @NotNull ComponentDialogBase<DialogButton> sendButton;

    /**
     * Set by the first Send that goes ahead. A double click, or Space on the
     * focused button, reaches {@link #submit} again.
     */
    private boolean sent;

    ReportBugDialog(final @NotNull Project p, final @NotNull BugReports.RunItem item, final @NotNull PreparedBug bug, final @NotNull Runnable redraw) {
        super(p);
        this.item = item;
        this.bug = bug;
        this.redraw = redraw;

        // What was edited and not sent comes back; otherwise the template.
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
                        .row(Bundle.message("bug.dialog.screenshots"), String.valueOf(bug.facts().stacktrace().screenshots().size()))
                        .row(Bundle.message("bug.dialog.repository"), bug.repository().map(BugRepository::displayName).orElse(""))
                        .build(),
                sendButton);
        shortcuts = List.of(StatusBarShortcut.cancel(this::closeCancel));

        titleField.getComponent().onTextChanged(this::refreshSend);
        bodyArea.getComponent().onTextChanged(this::refreshSend);
        refreshSend();
    }

    /**
     * Shows the dialog, and lets the run item go however it closes. A cancel
     * throws the edits away - Escape has already asked - and Send keeps them
     * until the issue exists.
     */
    void open() {
        show();
        getPopup().addListener(new JBPopupListener() {
            @Override
            public void onClosed(final @NotNull LightweightWindowEvent event) {
                final @NotNull BugReports reports = Services.getInstance(p, BugReports.class);
                if (!event.isOk()) reports.discard(item);

                reports.end(item, BugReports.Stage.OPEN);
                redraw.run();
            }
        });
    }

    private void refreshSend() {
        sendButton.getComponent().enableUnless(whyNot());
    }

    /**
     * Why Send will not send, as the tester types: what preparing found, and
     * what GitHub will not take.
     */
    private @NotNull Optional<String> whyNot() {
        return bug.whyNotReady().or(() -> BugLimits.whyNot(titleField.getComponent().getText(), bodyArea.getComponent().getText(),
                bug.facts().stacktrace().screenshots().size()));
    }

    /**
     * The run item is read again at the click: a test case passed while this
     * was open is not reported, and the dialog stays open saying so.
     */
    private @NotNull Optional<String> noLongerFailed() {
        return item.stillFailed(Services.getInstance(p, ProjectIndexer.class)).isPresent()
                ? Optional.empty()
                : Optional.of(Bundle.message("bug.no.longer.failed"));
    }

    @Override
    protected void submit() {
        if (sent) return;

        final @NotNull Optional<String> whyNot = whyNot().or(this::noLongerFailed);
        sendButton.getComponent().enableUnless(whyNot);
        if (whyNot.isPresent()) return;

        bug.repository().ifPresent(repository -> {
            sent = true;
            final @NotNull BugReports.Edits edits = new BugReports.Edits(titleField.getComponent().getText().strip(), bodyArea.getComponent().getText());
            BugFiling.send(p, item, repository, edits, bug.facts().stacktrace().screenshots(), redraw);
            closeOk();
        });
    }

    @Override
    protected boolean holdsUnsavedInput() {
        return !titleField.getComponent().getText().equals(bug.facts().title()) || !bodyArea.getComponent().getText().equals(bug.body());
    }

    @Override
    protected @NotNull String unsavedInputMessage() {
        return Bundle.message("bug.dialog.unsaved");
    }
}
