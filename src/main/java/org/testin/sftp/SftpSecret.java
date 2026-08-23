package org.testin.sftp;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.Objects;

/**
 * A secret this machine keeps for a server, in the IDE's own store (#94).
 * <p>
 * Never in {@code testin.yml}, which is committed and shared with everyone who
 * clones. Never in a marker, which travels with the test data. Never in the log.
 * {@link PasswordSafe} is Windows Credential Manager here, and the OS keychain
 * elsewhere - the same place the IDE keeps the credentials it already has for
 * the tester's Git remotes.
 * <p>
 * Not the environment either, which was considered: any process running as the
 * tester can read it, the IDE's own diagnostic collection captures it, and on
 * Windows a newly set variable is not visible to an IDE that was already
 * running - so it would fail with nothing to explain why.
 * <p>
 * Kept per server and per account, so two servers, or two accounts on one, do
 * not overwrite each other. The key names both and never contains the secret.
 * <p>
 * Asked only when the SSH agent cannot answer instead - see {@link SshAgent}.
 * <p>
 * <b>Never on the EDT.</b> Reading and writing go to the OS keychain, which the
 * platform counts as a slow operation and refuses there. A dialog's own callback
 * is the easy place to get this wrong, because it looks like the moment the
 * tester handed the secret over.
 */
@Getter
@AllArgsConstructor
public enum SftpSecret {

    /**
     * The passphrase of an encrypted key file, wanted only when no agent holds
     * that key already.
     */
    KEY_PASSPHRASE("key passphrase"),

    /**
     * The account's password, for a server that authenticates that way.
     */
    ACCOUNT_PASSWORD("account password");

    /**
     * The subsystem every entry is filed under, so a tester looking in Windows
     * Credential Manager can see which of them this plugin put there.
     */
    private static final @NotNull String SUBSYSTEM = "Testin";

    /**
     * What this secret is, in the words the tester is asked in and the words the
     * credential store lists it under.
     */
    private final @NotNull String description;

    /**
     * How this secret is filed for that server and account.
     * <p>
     * Pure, and separate from the store, so what a key looks like can be checked
     * without an IDE running - the store itself needs one and cannot be reached
     * from a unit test.
     */
    public @NotNull String keyFor(final @NotNull SftpAddress address, final @NotNull String user) {
        return "sftp://" + user + "@" + address.display() + " " + description;
    }

    /**
     * What is stored for that server and account, and empty when nothing is.
     * <p>
     * Empty rather than absent, so a caller hands it straight to the connection
     * and lets the server refuse it - which is the honest failure. "You have not
     * saved a passphrase" is a guess about which of several things went wrong.
     */
    public @NotNull String read(final @NotNull SftpAddress address, final @NotNull String user) {
        try {
            return Objects.requireNonNullElse(PasswordSafe.getInstance().getPassword(attributes(address, user)), "");
        } catch (final RuntimeException ex) {
            Logger.warn("Could not read the " + description + " from the credential store: " + ex.getMessage());
            return "";
        }
    }

    /**
     * Keeps it for next time. Answers whether the store took it - a tester whose
     * keychain refused should be told, rather than asked again every sync with
     * no explanation.
     */
    public boolean store(final @NotNull SftpAddress address, final @NotNull String user,
                         final @NotNull String secret) {
        try {
            PasswordSafe.getInstance().setPassword(attributes(address, user), secret);
            Logger.info("Stored the " + description + " for " + user + "@" + address.display());
            return true;
        } catch (final RuntimeException ex) {
            Logger.warn("Could not store the " + description + ": " + ex.getMessage());
            return false;
        }
    }

    /**
     * Removes it, for a tester who got it wrong or who wants it gone.
     */
    public void forget(final @NotNull SftpAddress address, final @NotNull String user) {
        try {
            PasswordSafe.getInstance().set(attributes(address, user), null);
        } catch (final RuntimeException ex) {
            Logger.warn("Could not remove the " + description + ": " + ex.getMessage());
        }
    }

    private @NotNull CredentialAttributes attributes(final @NotNull SftpAddress address,
                                                     final @NotNull String user) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName(SUBSYSTEM, keyFor(address, user)), user);
    }
}
