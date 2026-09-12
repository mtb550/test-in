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

package org.testin.sftp;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;
import org.testin.util.Shortcuts;

import java.util.List;
import java.util.function.Consumer;

/**
 * Asks who this machine is on the server, and how it proves it (#94).
 * <p>
 * Opened by the sync when it has a server address but no account - the address
 * comes from {@code testin.yml}, which is committed and shared, and the account
 * is the tester's own. Never the other way round: one account written into that
 * file would be everybody's, and a tester's own would be wrong for everybody
 * else.
 * <p>
 * The password is asked for only when nothing else can prove who this is. The
 * SSH agent is tried first, and a key file second, so on most machines this
 * dialog is never seen at all.
 */
final class SftpAccountDialog extends AbstractFrameworkDialog<TextInput> {

    private final @NotNull TextInput userField;
    private final @NotNull TextInput passwordField;
    private final @NotNull Consumer<@NotNull Account> onGiven;

    SftpAccountDialog(final @NotNull Project p, final @NotNull SftpAddress address, final @NotNull String knownUser, final @NotNull Consumer<@NotNull Account> onGiven) {
        super(p);
        this.onGiven = onGiven;

        title = Bundle.message("dialog.sftp.title", address.display());

        final @NotNull ComponentDialogBase<TextInput> user = ComponentDialogBase.textField()
                .placeholder(Bundle.message("dialog.sftp.placeholder.account"))
                .value(knownUser)
                .build();
        // UC-SHARE-019, Rule-SHARE-110. Dots, not characters: a tester on a
        // shared screen, a projector or a recorded session was showing their
        // server password (#66, finding 64).
        final @NotNull ComponentDialogBase<TextInput> password = ComponentDialogBase.textField()
                .placeholder(Bundle.message("dialog.sftp.placeholder.password"))
                .secret()
                .build();

        components = List.of(
                ComponentDialogBase.message(Bundle.message("dialog.sftp.message")),
                user,
                password);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.sftp.button.connect"), this::submit),
                StatusBarShortcut.cancel(this::closeCancel));

        userField = user.getComponent();
        passwordField = password.getComponent();
    }

    // UC-SHARE-019
    @Override
    protected void submit() {
        final @NotNull String user = accepted(userField);
        if (user.isEmpty()) return;

        onGiven.accept(new Account(user, passwordField.getText()));
        closeOk();
    }

    /**
     * What the tester gave. An empty password is a real answer - it means a key
     * or the agent is expected to do the proving.
     */
    record Account(@NotNull String user, @NotNull String password) {
    }
}
