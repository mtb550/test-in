package org.testin.sftp;

import com.jcraft.jsch.AgentConnector;
import com.jcraft.jsch.AgentIdentityRepository;
import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.PageantConnector;
import com.jcraft.jsch.SSHAgentConnector;
import com.jcraft.jsch.WindowsSSHAgentConnector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The running SSH agent, when there is one (#94).
 * <p>
 * Asked first, because an agent already holding the tester's key makes the
 * passphrase somebody else's problem: the key never leaves the agent, the
 * plugin never sees the secret, and there is nothing to prompt for or store.
 * That is also how the tester's own {@code ssh} and the IDE's Git behave, so
 * Testin asking for a passphrase when nothing else does would be the odd one.
 * <p>
 * Three agents are worth asking about on the machines this plugin runs on:
 * Windows OpenSSH, PuTTY's Pageant, and the Unix socket that
 * {@code SSH_AUTH_SOCK} names. They are tried in that order and the first that
 * answers wins; a machine with none simply has none, which is not a failure.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SshAgent {

    /**
     * Constructed lazily one at a time: each of these throws when the agent it
     * speaks to is not on this machine, which is the ordinary case for two of
     * the three and not something to report.
     */
    private static final @NotNull List<Supplier<AgentConnector>> CONNECTORS = List.of(
            SshAgent::windowsOpenSsh, SshAgent::pageant, SshAgent::unixSocket);

    /**
     * The identities a running agent is holding, and empty when there is no
     * agent or it is holding none.
     * <p>
     * Holding none is the same answer as having no agent: either way the agent
     * cannot get this connection in, and the key file has to. Asked here rather
     * than discovered as an authentication failure, because "the agent is
     * running but empty" and "your key is wrong" need different things from the
     * tester.
     */
    public static @NotNull Optional<IdentityRepository> loadedIdentities() {
        return available().<IdentityRepository>map(AgentIdentityRepository::new)
                .filter(repository -> !repository.getIdentities().isEmpty());
    }

    /**
     * An agent that is running and reachable, or empty when none is.
     */
    public static @NotNull Optional<AgentConnector> available() {
        for (final Supplier<AgentConnector> candidate : CONNECTORS) {
            final @NotNull Optional<AgentConnector> connector = tried(candidate);
            if (connector.isPresent()) {
                Logger.info("Using the SSH agent: " + connector.orElseThrow().getName());
                return connector;
            }
        }

        Logger.debug("No SSH agent is running, so a key will need its passphrase");
        return Optional.empty();
    }

    /**
     * One candidate, and empty when it is not there - which is what its
     * constructor says by throwing, and what {@code isAvailable} says when the
     * agent exists but is not running.
     */
    private static @NotNull Optional<AgentConnector> tried(final @NotNull Supplier<AgentConnector> candidate) {
        try {
            final @NotNull AgentConnector connector = candidate.get();

            return connector.isAvailable() ? Optional.of(connector) : Optional.empty();
        } catch (final Exception notOnThisMachine) {
            return Optional.empty();
        }
    }

    private static @NotNull AgentConnector windowsOpenSsh() {
        try {
            return new WindowsSSHAgentConnector();
        } catch (final Exception ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static @NotNull AgentConnector pageant() {
        try {
            return new PageantConnector();
        } catch (final Exception ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    private static @NotNull AgentConnector unixSocket() {
        try {
            return new SSHAgentConnector();
        } catch (final Exception ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }
}
