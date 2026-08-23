package org.testin.sftp;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * The transport, against a real SFTP server (#94).
 * <p>
 * Not a mock. Every one of these runs a genuine SSH conversation with a server
 * in this JVM, so what is proved is what the plugin will actually do to a
 * tester's machine - including the two behaviors the sync leans on hardest: that
 * a missing thing is not a failure, and that making a directory succeeds exactly
 * once.
 */
public class SftpTransportTest {

    private SftpTestServer server;
    private Path knownHosts;

    @BeforeMethod
    public void startServer() throws Exception {
        server = SftpTestServer.start();
        knownHosts = Files.createTempFile("testin-known-hosts", "");
        Files.writeString(knownHosts, server.knownHostsLine());
    }

    @AfterMethod
    public void stopServer() throws IOException {
        if (server != null) server.close();
        if (knownHosts != null) Files.deleteIfExists(knownHosts);
    }

    private SftpTransport connect() {
        return connectTo("");
    }

    private SftpTransport connectTo(final String path) {
        return SftpTransport.open(
                new SftpAddress("127.0.0.1", server.port(), path),
                SftpTestServer.USER,
                SftpAuth.withPassword(SftpTestServer.PASSWORD),
                knownHosts);
    }

    @Test
    public void whatIsWrittenIsWhatIsRead() {
        try (SftpTransport transport = connect()) {
            final byte[] content = "{\"description\":\"Sign in\"}".getBytes(StandardCharsets.UTF_8);
            transport.write("case.json", content);

            assertEquals(transport.read("case.json"), content);
        }
    }

    /**
     * The shape a test project actually has: folders with spaces in their names,
     * three deep, and none of them there before the write.
     */
    @Test
    public void foldersAreMadeOnTheWayToTheFile() {
        try (SftpTransport transport = connect()) {
            transport.write("Test Cases/pkg1/Login/6197ec6e.json", "{}".getBytes(StandardCharsets.UTF_8));

            assertTrue(transport.exists("Test Cases/pkg1/Login/6197ec6e.json"));
            assertTrue(Files.isDirectory(server.root().resolve("Test Cases").resolve("pkg1").resolve("Login")));
        }
    }

    @Test
    public void aProjectCanLiveUnderAFolderOnTheServer() {
        try (SftpTransport transport = connectTo("/srv/testin")) {
            transport.write("test-01/.tp", "{}".getBytes(StandardCharsets.UTF_8));

            assertTrue(Files.exists(server.root().resolve("srv").resolve("testin").resolve("test-01").resolve(".tp")),
                    "the address's own folder is where everything goes");
        }
    }

    @Test
    public void everyFileIsFoundAtEveryDepthIncludingTheMarkers() {
        try (SftpTransport transport = connect()) {
            transport.write(".tp", "{}".getBytes(StandardCharsets.UTF_8));
            transport.write("Test Cases/.tcd", "{}".getBytes(StandardCharsets.UTF_8));
            transport.write("Test Cases/pkg1/.tsp", "{}".getBytes(StandardCharsets.UTF_8));
            transport.write("Test Cases/pkg1/Login/a.json", "{}".getBytes(StandardCharsets.UTF_8));

            final List<String> found = transport.filesUnder("");

            assertEquals(found.size(), 4, "found: " + found);
            assertTrue(found.contains(".tp"), "a marker is a dotfile, and the indexer needs every one");
            assertTrue(found.contains("Test Cases/pkg1/Login/a.json"));
        }
    }

    @Test
    public void aFolderThatIsNotThereHoldsNoFiles() {
        try (SftpTransport transport = connect()) {
            assertEquals(transport.filesUnder("nothing-here"), List.of(),
                    "a first sync to an empty server is the ordinary case, not a failure");
        }
    }

    @Test
    public void deletingSomethingThatIsAlreadyGoneIsNotAFailure() {
        try (SftpTransport transport = connect()) {
            transport.write("case.json", "{}".getBytes(StandardCharsets.UTF_8));
            transport.delete("case.json");
            assertFalse(transport.exists("case.json"));

            transport.delete("case.json");
            assertFalse(transport.exists("case.json"), "the sync wanted it gone, and it is");
        }
    }

    /**
     * The lock, and the only atomic thing SFTP offers.
     * <p>
     * Two testers syncing at the same moment both call this; exactly one is told
     * it made the directory. If both were, both would upload over each other.
     */
    @Test
    public void makingADirectorySucceedsExactlyOnce() {
        try (SftpTransport transport = connect()) {
            assertTrue(transport.makeDirectory(".testin-lock"), "the first caller takes it");
            assertFalse(transport.makeDirectory(".testin-lock"), "the second is told somebody has it");
        }
    }

    /**
     * A host this machine has never seen is refused, even though everything else
     * about the connection is correct.
     */
    @Test
    public void anUnknownHostIsRefused() throws IOException {
        Files.writeString(knownHosts, "");

        final IllegalStateException refused = expectThrows(IllegalStateException.class, this::connect);
        assertTrue(refused.getMessage().contains("Could not connect"), refused.getMessage());
    }

    @Test
    public void aWrongPasswordIsRefused() {
        final IllegalStateException refused = expectThrows(IllegalStateException.class, () ->
                SftpTransport.open(new SftpAddress("127.0.0.1", server.port(), ""),
                        SftpTestServer.USER, SftpAuth.withPassword("wrong"), knownHosts));

        assertTrue(refused.getMessage().contains("Could not connect"), refused.getMessage());
    }
}
