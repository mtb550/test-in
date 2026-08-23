package org.testin.sftp;

import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.util.List;

/**
 * A real SFTP server on this machine, for trying the sync by hand.
 * <p>
 * Windows will not install OpenSSH Server without an administrator, and on a
 * managed machine it may refuse entirely - so this is the way to have something
 * to sync against without asking anyone for anything. It is the same server the
 * automated tests use, kept running instead of started and stopped.
 * <p>
 * Test scope: it is never in the distribution, and nothing in the plugin can
 * reach it.
 * <p>
 * Started with {@code gradlew sftpServer}. It keeps its host key and its files
 * between runs, so the {@code known_hosts} entry it writes stays valid - a fresh
 * key every time would look exactly like somebody impersonating the server, and
 * every later connection would be refused.
 */
public final class SftpServerRunner {

    private static final int PORT = 22;
    private static final String USER = "tester";
    private static final String PASSWORD = "testin";

    /**
     * The folder on the server that holds the projects, named after the Testin
     * root it mirrors.
     */
    private static final String TESTIN_ROOT = "Testin";

    public static void main(final String[] args) {

        try {
            final Path home = Path.of(System.getProperty("user.home"), ".testin-sftp");
            Files.createDirectories(home);

            // Also to a file, because when this starts with Windows there is no
            // console to read - and "it is not working" needs somewhere to look.
            log(home, "starting");
            final Path root = Files.createDirectories(home.resolve("srv"));
            final Path hostKey = home.resolve("host.ser");

            // The Testin root, mirrored. A tester's machine has one folder holding a
            // list of projects, and the server holds the same - so what is on it can
            // be read without translating anything, and a second Testin root could be
            // pointed straight at it.
            final Path testinRoot = Files.createDirectories(root.resolve(TESTIN_ROOT));

            final SshServer server = SshServer.setUpDefaultServer();
            server.setHost("127.0.0.1");
            server.setPort(PORT);
            server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey));
            server.setPasswordAuthenticator((user, password, session) ->
                    USER.equals(user) && PASSWORD.equals(password));
            server.setPublickeyAuthenticator((user, key, session) -> USER.equals(user));
            server.setSubsystemFactories(List.of(new SftpSubsystemFactory()));
            server.setFileSystemFactory(new VirtualFileSystemFactory(root));
            server.start();

            final String knownHostsLine = trust(server);

            System.out.println();
            System.out.println("  SFTP server running.");
            System.out.println("  ---------------------------------------------------------------");
            System.out.println("  address for testin.yml : sftp://127.0.0.1" + (PORT == 22 ? "" : ":" + PORT) + "/" + TESTIN_ROOT);
            System.out.println("  account                : " + USER);
            System.out.println("  password               : " + PASSWORD);
            System.out.println("  it serves              : " + testinRoot);
            System.out.println("  host key trusted in    : " + knownHostsFile());
            System.out.println("  ---------------------------------------------------------------");
            System.out.println("  " + knownHostsLine.trim());
            System.out.println();
            System.out.println("  Watch " + testinRoot + " to see what the plugin sends.");
            System.out.println("  Ctrl+C to stop.");
            System.out.println();

            log(home, "listening on 127.0.0.1:" + PORT + ", serving " + testinRoot);
            Thread.currentThread().join();

        } catch (final Exception ex) {

            throw new AssertionError(ex);

        }

    }

    /**
     * One line in the server's own log, beside the files it serves.
     */
    private static void log(final Path home, final String line) {
        try {
            Files.writeString(home.resolve("server.log"),
                    java.time.LocalDateTime.now().withNano(0) + "  " + line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final Exception ignored) {
            // A server that cannot write its log still serves files.
        }
    }

    /**
     * How the host is written in {@code known_hosts}.
     * <p>
     * A plain name on the default port, and bracketed with the port on any
     * other - which is what OpenSSH writes and what the client looks up. Get
     * this wrong and the entry is simply never found, so every connection is
     * refused as an unknown host with nothing to say why.
     */
    private static String host() {
        return PORT == 22 ? "127.0.0.1" : "[127.0.0.1]:" + PORT;
    }

    private static Path knownHostsFile() {
        return Path.of(System.getProperty("user.home"), ".ssh", "known_hosts");
    }

    /**
     * Puts this server's host key in {@code known_hosts}, once.
     * <p>
     * The client refuses a host it has not seen, deliberately, so without this
     * every connection would fail. Added rather than switched off, because a
     * test that connects with the check disabled proves the transport works in a
     * way the plugin must never run.
     */
    private static String trust(final SshServer server) {
        try {
            final KeyPair pair = server.getKeyPairProvider().loadKeys(null).iterator().next();
            final String line = host() + " " + PublicKeyEntry.toString(pair.getPublic()) + System.lineSeparator();

            final Path file = knownHostsFile();
            Files.createDirectories(file.getParent());

            final String existing = Files.exists(file) ? Files.readString(file) : "";
            if (existing.contains(line.trim())) return line;

            final String separator = existing.isEmpty() || existing.endsWith("\n") ? "" : System.lineSeparator();
            Files.writeString(file, separator + line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            return line;
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
