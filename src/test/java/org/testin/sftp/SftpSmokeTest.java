package org.testin.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * That the client the plugin ships and a real SSH server can hold a conversation
 * (#94).
 * <p>
 * Everything else in this feature is built on top of this working, so it is
 * proved first and on every build: key exchange, password authentication, the
 * SFTP subsystem, a file put and got back byte for byte.
 * <p>
 * And the property that decided which library ships: an unknown host key is
 * refused. That is the reason this plugin does not use the server library as its
 * client, so it is worth a test rather than a sentence.
 */
public class SftpSmokeTest {

    private SftpTestServer server;

    @BeforeMethod
    public void startServer() {
        server = SftpTestServer.start();
    }

    @AfterMethod
    public void stopServer() {
        if (server != null) server.close();
    }

    /**
     * Opens a session that verifies the host, the way the plugin must.
     */
    private Session connectVerifying() {
        try {
            final JSch jsch = new JSch();
            jsch.setKnownHosts(new ByteArrayInputStream(server.knownHostsLine().getBytes(StandardCharsets.UTF_8)));

            final Session session = jsch.getSession(SftpTestServer.USER, "127.0.0.1", server.port());
            session.setPassword(SftpTestServer.PASSWORD.getBytes(StandardCharsets.UTF_8));
            session.connect(10_000);

            return session;
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aFilePutOnTheServerComesBackByteForByte() {
        try {
            final Session session = connectVerifying();
            final ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10_000);

            try {
                final byte[] content = "{\"description\":\"Sign in\"}".getBytes(StandardCharsets.UTF_8);
                sftp.put(new ByteArrayInputStream(content), "case.json");

                assertTrue(Files.exists(server.root().resolve("case.json")), "the server should hold it");
                assertEquals(Files.readAllBytes(server.root().resolve("case.json")), content);
                assertEquals(sftp.get("case.json").readAllBytes(), content, "and it should come back unchanged");
            } finally {
                sftp.disconnect();
                session.disconnect();
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void directoriesCanBeMadeAndListed() {
        try {
            final Session session = connectVerifying();
            final ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10_000);

            try {
                // mkdir is the one operation SFTP makes atomic, which is what the
                // sync lock will be built on.
                sftp.mkdir("Test Cases");
                assertTrue(Files.isDirectory(server.root().resolve("Test Cases")), "a name with a space still works");

                sftp.put(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), "Test Cases/a.json");
                assertEquals(sftp.ls("Test Cases").size(), 3, ". and .. and the file");
            } finally {
                sftp.disconnect();
                session.disconnect();
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The reason this client ships and the server library does not.
     * <p>
     * With no {@code known_hosts} to check against, the connection must not be
     * made. A client that connected here would connect to anything claiming to
     * be the server, which is what host keys exist to prevent.
     */
    @Test
    public void anUnknownHostKeyIsRefused() {
        final JSch jsch = new JSch();

        try {
            final Session session = jsch.getSession(SftpTestServer.USER, "127.0.0.1", server.port());
            session.setPassword(SftpTestServer.PASSWORD.getBytes(StandardCharsets.UTF_8));
            session.connect(10_000);
            session.disconnect();

            fail("connected to a host it had never seen");
        } catch (final JSchException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("hostkey")
                            || expected.getMessage().toLowerCase().contains("host key"),
                    "refused, but not for the host key: " + expected.getMessage());
        }
    }

    /**
     * And that switching the check off is a deliberate act, not the default -
     * so the plugin never gets there by omission.
     */
    @Test
    public void theCheckHasToBeTurnedOffOnPurpose() {
        try {
            final JSch jsch = new JSch();
            final Session session = jsch.getSession(SftpTestServer.USER, "127.0.0.1", server.port());
            session.setPassword(SftpTestServer.PASSWORD.getBytes(StandardCharsets.UTF_8));

            final Properties off = new Properties();
            off.put("StrictHostKeyChecking", "no");
            session.setConfig(off);
            session.connect(10_000);

            try {
                assertTrue(session.isConnected());
            } finally {
                session.disconnect();
            }
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }
}
