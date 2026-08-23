package org.testin.sftp;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.ComponentDialogBase;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
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

    SftpAccountDialog(final @NotNull Project p, final @NotNull SftpAddress address,
                        final @NotNull String knownUser, final @NotNull Consumer<@NotNull Account> onGiven) {
        super(p);
        this.onGiven = onGiven;

        title = "Connect to " + address.display();

        final @NotNull ComponentDialogBase<TextInput> user = ComponentDialogBase.textField()
                .placeholder("account on the server...")
                .value(knownUser)
                .build();
        final @NotNull ComponentDialogBase<TextInput> password = ComponentDialogBase.textField()
                .placeholder("password, if no key is set up...")
                .build();

        components = List.of(
                ComponentDialogBase.message("The server address comes from testin.yml and is shared with the team. "
                        + "The account is yours, and is kept on this machine only."),
                user,
                password);

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, "Connect", this::submit),
                StatusBarShortcut.build(Shortcuts.Escape, "Cancel", this::closeCancel));

        userField = user.getComponent();
        passwordField = password.getComponent();
    }

    @Override
    protected void submit() {
        final @NotNull String user = userField.getText().trim();
        if (user.isEmpty()) {
            userField.showEmptyWarning();
            return;
        }

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
