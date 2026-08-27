package org.testin.sftp;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * One sync at a time against a project on the server (#94).
 * <p>
 * What this protects is the record rather than the transfer. Two testers
 * pressing Sync in the same minute both read the manifest, both decide from it,
 * and both write it back - so the second one's manifest describes a server that
 * also holds the first one's files, and the sync after that reads the difference
 * as somebody's deletions. Nobody would ever see the cause; they would see test
 * cases disappearing.
 * <p>
 * Run against a real server, because the property being relied on is the
 * server's: that two clients calling mkdir on one path cannot both be told they
 * made it. Asserting that against a mock would only be asserting the mock.
 */
public class SyncLockTest {

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

    @Test
    public void theFirstTesterTakesItAndTheSecondIsRefused() {
        try (SftpTransport mine = connect(); SftpTransport theirs = connect()) {
            assertTrue(new SyncLock(mine).takenBy("muteb").isEmpty(), "nobody had it");

            final Optional<String> refused = new SyncLock(theirs).takenBy("sara");

            assertTrue(refused.isPresent(), "two syncs writing one manifest is how a sync loses files");
            assertTrue(refused.orElseThrow().startsWith("muteb"),
                    "the tester who is refused is told who to wait for: " + refused.orElseThrow());
        }
    }

    @Test
    public void theRefusalSaysWhenItStarted() {
        try (SftpTransport mine = connect(); SftpTransport theirs = connect()) {
            new SyncLock(mine).takenBy("muteb");

            final String refused = new SyncLock(theirs).takenBy("sara").orElseThrow();

            assertTrue(refused.contains("started a sync at"), refused);
        }
    }

    @Test
    public void releasingItLetsTheNextSyncThrough() {
        try (SftpTransport mine = connect(); SftpTransport theirs = connect()) {
            final SyncLock lock = new SyncLock(mine);
            lock.takenBy("muteb");
            lock.release();

            assertTrue(new SyncLock(theirs).takenBy("sara").isEmpty(),
                    "a lock that is not given back blocks the whole team until somebody deletes it over SSH");
        }
    }

    @Test
    public void releasingALockNobodyHoldsIsNotAFailure() {
        try (SftpTransport transport = connect()) {
            new SyncLock(transport).release();
            new SyncLock(transport).release();

            assertTrue(new SyncLock(transport).takenBy("muteb").isEmpty(),
                    "tidying up after a sync that already tidied up leaves the project usable");
        }
    }

    @Test
    public void theSameTesterSyncingTwiceAtOnceIsStillTwoSyncs() {
        try (SftpTransport first = connect(); SftpTransport second = connect()) {
            new SyncLock(first).takenBy("muteb");

            assertFalse(new SyncLock(second).takenBy("muteb").isEmpty(),
                    "two windows on one machine race exactly as two machines do");
        }
    }
}
