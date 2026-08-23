package org.testin.sftp;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.jetbrains.annotations.NotNull;

/**
 * How this machine proves who it is to the server (#94).
 * <p>
 * An interface with an implementation each rather than a flag the transport
 * tests, so opening a connection reads the same line whichever way a tester
 * authenticates - the reason {@code GenAction} and {@code RemoveHandler} are
 * interfaces here too.
 * <p>
 * Nothing here is written to {@code testin.yml}, or to a marker, or to the log.
 * That file is committed and a marker travels with the test data; a passphrase
 * belongs in the IDE's own password store and nowhere else.
 */
@FunctionalInterface
public interface SftpAuth {

    /**
     * A machine that offers nothing, so the server refuses it.
     * <p>
     * What an unconfigured connection carries. It fails at the server rather
     * than at a check here, which is the honest place: "the server would not
     * accept this" is true, and "you have not set a password yet" is a guess
     * about which of several reasons applied.
     */
    @NotNull
    SftpAuth NONE = (jsch, session) -> {
    };

    /**
     * A key file, and its passphrase when it has one.
     * <p>
     * The passphrase is passed to the library and never held here. An empty one
     * means the key is not encrypted, which is a real state and not a missing
     * value.
     */
    static @NotNull SftpAuth withKey(final @NotNull String keyFile, final @NotNull String passphrase) {
        return (jsch, session) -> jsch.addIdentity(keyFile, passphrase);
    }

    static @NotNull SftpAuth withPassword(final @NotNull String password) {
        return (jsch, session) -> session.setPassword(password);
    }

    /**
     * The agent signs, and this machine never holds the key.
     */
    static @NotNull SftpAuth withAgent(final @NotNull IdentityRepository identities) {
        return (jsch, session) -> jsch.setIdentityRepository(identities);
    }

    /**
     * How a key-authenticated connection proves itself: the agent when one is
     * holding keys, and the key file when none is.
     * <p>
     * The agent first because it settles the passphrase question rather than
     * answering it - the key never leaves the agent and nothing has to be
     * stored or asked for. It is also what the tester's own {@code ssh} and the
     * IDE's Git already do, so Testin prompting when neither of them does would
     * be the odd one out.
     * <p>
     * The passphrase is fetched only if it is going to be used, which is why it
     * arrives as something to call rather than as a value: reading the
     * credential store costs a keychain round trip, and on most machines the
     * agent means it is never needed.
     */
    static @NotNull SftpAuth forKey(final @NotNull String keyFile,
                                      final @NotNull java.util.function.Supplier<String> passphrase) {
        return SshAgent.loadedIdentities()
                .map(SftpAuth::withAgent)
                .orElseGet(() -> withKey(keyFile, passphrase.get()));
    }

    /**
     * Puts this machine's proof on the session before it connects.
     * <p>
     * Declares {@code throws} because the library does, and because a
     * functional interface whose whole point is to let an implementation fail is
     * the one case this codebase allows it - the same reason {@code GitTaskWork}
     * does.
     */
    void apply(final @NotNull JSch jsch, final @NotNull Session session) throws Exception;
}
