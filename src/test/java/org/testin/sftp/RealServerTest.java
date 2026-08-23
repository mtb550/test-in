package org.testin.sftp;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * The transport against the tester's own SFTP server, not an embedded one (#94).
 * <p>
 * {@link SftpSmokeTest} proves the client and a server can hold a conversation,
 * but it starts that server itself - same process, keys it generated, a port it
 * chose. What it cannot prove is that the machine in front of the tester is
 * reachable: that port 22 answers, that the account works, that the host key is
 * one this machine already trusts, and that the folders are where the file says.
 * Those are exactly the things that fail first on a real setup.
 * <p>
 * <b>Skipped unless a server is named</b>, because it needs an account and this
 * build runs on machines that have none. The password is passed in at the
 * command line and is never written to the repository, the log, or a file:
 * <pre>
 * ./gradlew test --tests "org.testin.sftp.RealServerTest" --rerun ^
 *     -Dtestin.sftp.host=127.0.0.1 -Dtestin.sftp.user=NAME -Dtestin.sftp.password=SECRET
 * </pre>
 * A key is used instead when {@code testin.sftp.key} names one.
 * <p>
 * Everything it writes goes under a folder of its own and is removed again, so
 * running it cannot disturb a real test project sitting beside it.
 */
public class RealServerTest {

    /**
     * The folder this test owns, under the root it is given. Named so that
     * finding one left behind says what left it.
     */
    private static final String SCRATCH = "testin-selftest";

    private SftpAddress address = SftpAddress.NONE;
    private String user = "";
    private SftpAuth auth = SftpAuth.NONE;
    private Path knownHosts = Paths.get("");

    @BeforeClass
    public void readTheServerFromTheCommandLine() {
        final String host = property("testin.sftp.host");
        final String name = property("testin.sftp.user");

        if (host.isEmpty() || name.isEmpty()) {
            throw new SkipException("No server given - pass -Dtestin.sftp.host and -Dtestin.sftp.user to run this");
        }

        final String root = property("testin.sftp.path", "/Testin");
        final String key = property("testin.sftp.key");
        final String password = property("testin.sftp.password");

        address = new SftpAddress(host, port(), root + "/" + SCRATCH);
        user = name;
        auth = key.isEmpty() ? SftpAuth.withPassword(password) : SftpAuth.withKey(key, property("testin.sftp.passphrase"));
        knownHosts = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts");
    }

    private static int port() {
        final String given = property("testin.sftp.port", "22");

        return Integer.parseInt(given);
    }

    private static String property(final String key) {
        return property(key, "");
    }

    private static String property(final String key, final String fallback) {
        final String value = System.getProperty(key, fallback);

        return value == null ? fallback : value.trim();
    }

    /**
     * The whole path a sync takes, against the real thing: connect, create the
     * project folder, put a file in a sub-folder that does not exist yet, read
     * it back byte for byte, list it, then remove it all.
     */
    @Test
    public void aFileSurvivesTheRoundTrip() {
        final byte[] content = "case,expected\nlogin,pass\n".getBytes(StandardCharsets.UTF_8);

        try (SftpTransport transport = SftpTransport.open(address, user, auth, knownHosts)) {
            transport.write("Test Cases/login.tcd", content);

            assertTrue(transport.exists("Test Cases/login.tcd"), "the server kept what was sent");
            assertEquals(transport.read("Test Cases/login.tcd"), content, "byte for byte");

            final List<String> found = transport.filesUnder("");
            assertTrue(found.contains("Test Cases/login.tcd"), "listed under the project, found " + found);

            transport.delete("Test Cases/login.tcd");
            assertFalse(transport.exists("Test Cases/login.tcd"), "and removed again");
        }
    }

    /**
     * A folder the server has never heard of is created on the way, rather than
     * failing - a first sync writes into nothing but the root.
     */
    @Test
    public void deepFoldersAreCreatedOnTheWay() {
        try (SftpTransport transport = SftpTransport.open(address, user, auth, knownHosts)) {
            transport.write("Suites/Regression/Smoke/one.json", "{}".getBytes(StandardCharsets.UTF_8));

            assertTrue(transport.exists("Suites/Regression/Smoke/one.json"));
            transport.delete("Suites/Regression/Smoke/one.json");
        }
    }

    /**
     * The host key is checked against this machine's {@code known_hosts}, on the
     * real server as everywhere else.
     * <p>
     * Worth its own test here rather than only against the embedded server: the
     * embedded one is trusted by a line the test wrote itself, which proves the
     * check runs but not that the tester's own file is where the plugin looks.
     */
    @Test
    public void theHostIsOneThisMachineAlreadyTrusts() {
        try (SftpTransport transport = SftpTransport.open(address, user, auth, knownHosts)) {
            assertTrue(transport.exists(""), "connected, so the key in known_hosts matched");
        }
    }
}
