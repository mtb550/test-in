package org.testin.sftp;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What a sync decides, against a server that is really running (#94).
 * <p>
 * The pieces each have a test of their own - the transport moves a file, the
 * manifest survives JSON, the truth table maps three hashes to one action. What
 * none of them covers is the three together, which is the only arrangement a
 * tester ever meets: files go up, the server is asked what it now holds, and the
 * next sync has to say "nothing to do" rather than sending all of it again.
 * <p>
 * That last property is the whole design. The first sync is expensive - 2,246
 * files is 2,246 round trips - and it is only bearable because every sync after
 * it moves what changed and nothing else. A regression there is invisible in a
 * test of any one piece and costs a minute and a half every time somebody
 * presses Sync.
 * <p>
 * The decision under test is the shipping one, reached through
 * {@link SftpSync#wouldDo}, rather than a copy of it written here.
 */
public class SyncOverServerTest {

    private static final String CASE = "Test Cases/Login/6197ec6e.json";

    private SftpTestServer server;
    private Path knownHosts;

    @BeforeMethod
    public void startServer() {
        try {
            server = SftpTestServer.start();
            knownHosts = Files.createTempFile("testin-known-hosts", "");
            Files.writeString(knownHosts, server.knownHostsLine());
        } catch (final Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void stopServer() {
        try {
            if (server != null) server.close();
            if (knownHosts != null) Files.deleteIfExists(knownHosts);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private SftpTransport connect() {
        return SftpTestServer.connect(new SftpAddress("127.0.0.1", server.port(), "/projects/test-01"),
                SftpTestServer.USER, SftpAuth.withPassword(SftpTestServer.PASSWORD), knownHosts);
    }

    private static Map<String, byte[]> project() {
        final Map<String, byte[]> files = new TreeMap<>();
        files.put(".tp", bytes("{\"status\":\"ACTIVE\"}"));
        files.put("Test Cases/.tcd", bytes("{}"));
        files.put("Test Cases/Login/.ts", bytes("{\"order\":1}"));
        files.put(CASE, bytes("{\"description\":\"Sign in\"}"));

        return files;
    }

    private static byte[] bytes(final String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Everything the server holds, read back the way a sync reads it.
     */
    private Manifest onServer() {
        final Map<String, byte[]> held = new TreeMap<>();

        try (SftpTransport transport = connect()) {
            for (final String path : transport.filesUnder("")) {
                held.put(path, transport.read(path));
            }
        }

        return Manifest.of(held);
    }

    private void upload(final Map<String, byte[]> files) {
        try (SftpTransport transport = connect()) {
            files.forEach(transport::write);
        }
    }

    @Test
    public void aProjectTheServerHasNeverSeenGoesUpWhole() {
        final Map<String, byte[]> local = project();

        final SftpSync.Outcome first = SftpSync.wouldDo(Manifest.of(local), Manifest.EMPTY, Manifest.EMPTY);

        assertEquals(first.uploaded(), local.size(), "a server that knows nothing gets all of it");
        assertEquals(first.downloaded(), 0);
        assertEquals(first.conflicts(), 0, "nothing can be in conflict with a server that has nothing");
    }

    @Test
    public void theSyncAfterTheFirstOneMovesNothing() {
        final Map<String, byte[]> local = project();
        upload(local);

        final Manifest remote = onServer();
        assertEquals(remote.entries().keySet(), local.keySet(), "the server should hold exactly what went up");

        final SftpSync.Outcome second = SftpSync.wouldDo(Manifest.of(local), remote, Manifest.of(local));

        assertEquals(second.uploaded(), 0, "nothing changed here");
        assertEquals(second.downloaded(), 0, "nothing changed there");
        assertEquals(second.unchanged(), local.size(), "every file is settled");
        assertEquals(second.describe(), "Already up to date");
    }

    @Test
    public void onlyTheEditedCaseIsSentTheSecondTime() {
        final Map<String, byte[]> agreed = project();
        upload(agreed);

        final Manifest remote = onServer();

        final Map<String, byte[]> local = new TreeMap<>(agreed);
        local.put(CASE, bytes("{\"description\":\"Sign in with a bad password\"}"));

        final SftpSync.Outcome outcome = SftpSync.wouldDo(Manifest.of(local), remote, Manifest.of(agreed));

        assertEquals(outcome.uploaded(), 1, "one case was edited, so one case moves");
        assertEquals(outcome.unchanged(), agreed.size() - 1);
        assertEquals(outcome.describe(), "Sent 1");
    }

    @Test
    public void aCaseSomebodyElseEditedComesDown() {
        final Map<String, byte[]> agreed = project();
        upload(agreed);

        try (SftpTransport transport = connect()) {
            transport.write(CASE, bytes("{\"description\":\"Sign in, edited by somebody else\"}"));
        }

        final SftpSync.Outcome outcome = SftpSync.wouldDo(Manifest.of(agreed), onServer(), Manifest.of(agreed));

        assertEquals(outcome.downloaded(), 1, "this machine has not touched it, so the server's copy stands");
        assertEquals(outcome.uploaded(), 0);
        assertEquals(outcome.describe(), "Took 1");
    }

    @Test
    public void aCaseBothSidesEditedIsNeitherSentNorFetched() {
        final Map<String, byte[]> agreed = project();
        upload(agreed);

        try (SftpTransport transport = connect()) {
            transport.write(CASE, bytes("{\"description\":\"Theirs\"}"));
        }

        final Map<String, byte[]> local = new TreeMap<>(agreed);
        local.put(CASE, bytes("{\"description\":\"Mine\"}"));

        final SftpSync.Outcome outcome = SftpSync.wouldDo(Manifest.of(local), onServer(), Manifest.of(agreed));

        assertEquals(outcome.conflicts(), 1, "both moved, and not to the same place");
        assertEquals(outcome.conflicting(), List.of(CASE), "and the tester is told which one");
        assertEquals(outcome.uploaded(), 0, "guessing here overwrites work with no other copy");
        assertEquals(outcome.downloaded(), 0);
    }

    @Test
    public void aCaseDeletedOnTheServerIsReportedRatherThanTransferred() {
        final Map<String, byte[]> agreed = project();
        upload(agreed);

        try (SftpTransport transport = connect()) {
            transport.delete(CASE);
        }

        final SftpSync.Outcome outcome = SftpSync.wouldDo(Manifest.of(agreed), onServer(), Manifest.of(agreed));

        assertEquals(outcome.removedOnServer(), List.of(CASE));
        assertEquals(outcome.uploaded(), 0, "sending it back would undo a deletion somebody meant");
    }

    @Test
    public void aFileGitOwnsIsNoPartOfWhatIsShared() {
        final Map<String, byte[]> local = project();
        local.put(".git/HEAD", bytes("ref: refs/heads/main\n"));

        final SftpSync.Outcome outcome = SftpSync.wouldDo(Manifest.of(local), Manifest.EMPTY, Manifest.EMPTY);

        assertEquals(outcome.uploaded(), project().size(),
                "sftp is a separate way of sharing, not a mirror of a working tree");
        assertTrue(outcome.describe().startsWith("Sent "), outcome.describe());
    }
}
