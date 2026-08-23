package org.testin.sftp;

import org.jetbrains.annotations.NotNull;


/**
 * Where a test project is kept on a server (#94).
 * <p>
 * Parsed from the {@code testinSftp} value in {@code testin.yml}, which is
 * committed - so this carries the host, the port and the folder, and never an
 * account. Who connects is a fact about the person at the machine, and the file
 * is shared with everyone who clones it.
 *
 * @param host the machine to connect to
 * @param port its SSH port, 22 unless the address said otherwise
 * @param path the folder on it that holds the test projects, without a trailing
 *             separator so joining a name onto it needs no test for one
 */
public record SftpAddress(@NotNull String host, int port, @NotNull String path) {

    /**
     * The default SSH port, which an address is not required to state.
     */
    private static final int DEFAULT_PORT = 22;

    /**
     * No server configured, and equally an address that could not be read. Both
     * mean the same thing to every caller - there is nowhere to sync to - so
     * they are the same value rather than two states to tell apart.
     */
    public static final @NotNull SftpAddress NONE = new SftpAddress("", DEFAULT_PORT, "");

    /**
     * Whether there is a server to talk to.
     */
    public boolean isConfigured() {
        return !host.isEmpty();
    }

    /**
     * That file, on this server. Always forward slashes: SFTP paths are not
     * Windows paths, whatever machine the plugin is running on.
     * <p>
     * Never empty. An address with no folder, asked for the project root,
     * would otherwise produce "" - which is not a path any server can be asked
     * about, and which reached the library as an index into an empty string.
     * The folder a session opens in is ".", so that is what nothing resolves to.
     */
    public @NotNull String resolve(final @NotNull String relative) {
        final @NotNull String joined = path.isEmpty() ? relative : path + "/" + relative;

        return joined.isEmpty() ? "." : trailing(joined);
    }

    /**
     * Without a trailing separator, which asking for the root of a folder
     * otherwise leaves behind.
     */
    private static @NotNull String trailing(final @NotNull String joined) {
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }

    /**
     * How the address reads in a message to a tester, and in {@code known_hosts},
     * which writes a non-default port in brackets.
     */
    public @NotNull String display() {
        return port == DEFAULT_PORT ? host : "[" + host + "]:" + port;
    }
}
