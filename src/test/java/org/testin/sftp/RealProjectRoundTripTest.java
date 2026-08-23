package org.testin.sftp;

import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A real test project, all the way to a server and back (#94).
 * <p>
 * Everything else in this package proves a piece. This proves the pieces
 * together, against the data a tester actually has: every marker, every test
 * case, every folder with a space in its name, at the size it really is - and it
 * says how long that took, because the whole design rests on the claim that the
 * file count matters more than the bytes.
 * <p>
 * Skipped when the project it reads is not on this machine, so it is a gift on
 * the machine that has it and silent everywhere else. It only ever reads that
 * directory; everything written goes to a temporary server and a temporary
 * folder.
 */
public class RealProjectRoundTripTest {

    /**
     * The sandbox project this plugin is developed against. Read-only here.
     */
    private static final Path PROJECT = Path.of("C:", "Users", "mtb", "Downloads", "Testin", "test-01");

    private SftpTestServer server;
    private Path knownHosts;
    private Path downloaded;

    @BeforeMethod
    public void startServer() throws Exception {
        if (!Files.isDirectory(PROJECT)) {
            throw new SkipException("No test project at " + PROJECT + " on this machine");
        }

        server = SftpTestServer.start();
        knownHosts = Files.createTempFile("testin-known-hosts", "");
        Files.writeString(knownHosts, server.knownHostsLine());
        downloaded = Files.createTempDirectory("testin-downloaded");
    }

    @AfterMethod
    public void stopServer() throws IOException {
        if (server != null) server.close();
        if (knownHosts != null) Files.deleteIfExists(knownHosts);
        if (downloaded == null || !Files.exists(downloaded)) return;

        try (Stream<Path> paths = Files.walk(downloaded)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    private SftpTransport connect() {
        return SftpTransport.open(new SftpAddress("127.0.0.1", server.port(), "/projects/test-01"),
                SftpTestServer.USER, SftpAuth.withPassword(SftpTestServer.PASSWORD), knownHosts);
    }

    /**
     * Every file on this machine's copy of the project, by the path a manifest
     * names it with - forward slashes, relative to the project root. The Git
     * directory is not test data and is not part of what a server holds.
     */
    private static Map<String, byte[]> readProject() throws IOException {
        final Map<String, byte[]> files = new TreeMap<>();

        try (Stream<Path> paths = Files.walk(PROJECT)) {
            final List<Path> regular = paths.filter(Files::isRegularFile)
                    .filter(path -> !PROJECT.relativize(path).toString().replace('\\', '/').startsWith(".git/"))
                    .toList();

            for (final Path path : regular) {
                files.put(PROJECT.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
            }
        }

        return files;
    }

    @Test
    public void aWholeProjectSurvivesTheRoundTrip() throws Exception {
        final Map<String, byte[]> local = readProject();
        assertTrue(local.size() > 100, "this is meant to run against a real project, not a toy: " + local.size());

        final long uploadStarted = System.nanoTime();
        try (SftpTransport transport = connect()) {
            local.forEach(transport::write);
        }
        final long uploadMillis = (System.nanoTime() - uploadStarted) / 1_000_000;

        final long downloadStarted = System.nanoTime();
        final Map<String, byte[]> back = new TreeMap<>();
        try (SftpTransport transport = connect()) {
            for (final String path : transport.filesUnder("")) {
                back.put(path, transport.read(path));
            }
        }
        final long downloadMillis = (System.nanoTime() - downloadStarted) / 1_000_000;

        final long bytes = local.values().stream().mapToLong(content -> content.length).sum();
        System.out.println("[round trip] " + local.size() + " files, " + bytes + " bytes"
                + " | up " + uploadMillis + " ms, down " + downloadMillis + " ms");

        assertEquals(back.keySet(), local.keySet(), "the server should hold exactly what this machine does");

        final List<String> differing = new ArrayList<>();
        local.forEach((path, content) -> {
            if (!java.util.Arrays.equals(content, back.get(path))) differing.add(path);
        });
        assertEquals(differing, List.of(), "these came back different");
    }

    /**
     * The same project, the same server, as one bundle - against the
     * file-by-file figure above.
     * <p>
     * This is the comparison the transfer shape was decided on, run rather than
     * estimated.
     */
    @Test
    public void aBundleMovesTheWholeProjectInOneTransfer() throws Exception {
        final Map<String, byte[]> local = readProject();

        final long packStarted = System.nanoTime();
        final byte[] bundle = ProjectBundle.pack(local);
        final long packMillis = (System.nanoTime() - packStarted) / 1_000_000;

        final long upStarted = System.nanoTime();
        try (SftpTransport transport = connect()) {
            transport.write(".testin/project.tar", bundle);
        }
        final long upMillis = (System.nanoTime() - upStarted) / 1_000_000;

        final long downStarted = System.nanoTime();
        final byte[] fetched;
        try (SftpTransport transport = connect()) {
            fetched = transport.read(".testin/project.tar");
        }
        final long downMillis = (System.nanoTime() - downStarted) / 1_000_000;

        final long unpackStarted = System.nanoTime();
        final Map<String, byte[]> back = ProjectBundle.unpack(fetched);
        final long unpackMillis = (System.nanoTime() - unpackStarted) / 1_000_000;

        System.out.println("[bundle round trip] " + local.size() + " files as " + bundle.length + " bytes"
                + " | pack " + packMillis + " ms, up " + upMillis + " ms,"
                + " down " + downMillis + " ms, unpack " + unpackMillis + " ms");

        assertEquals(back.keySet(), local.keySet(), "the bundle should hold exactly what this machine does");

        final List<String> differing = new ArrayList<>();
        local.forEach((path, content) -> {
            if (!java.util.Arrays.equals(content, back.get(path))) differing.add(path);
        });
        assertEquals(differing, List.of(), "these came back different");
    }

    /**
     * And the manifest of what went up matches the manifest of what came back,
     * which is the comparison every future sync is decided by.
     */
    @Test
    public void theManifestsAgreeAfterARoundTrip() throws Exception {
        final Map<String, byte[]> local = readProject();

        try (SftpTransport transport = connect()) {
            local.forEach(transport::write);
        }

        final Map<String, byte[]> back = new TreeMap<>();
        try (SftpTransport transport = connect()) {
            for (final String path : transport.filesUnder("")) {
                back.put(path, transport.read(path));
            }
        }

        final Manifest before = Manifest.of(local);
        final Manifest after = Manifest.of(back);

        assertEquals(after.entries(), before.entries(), "a sync run now would think nothing had changed");
        assertEquals(after.totalBytes(), before.totalBytes());

        for (final String path : before.pathsWith(after)) {
            assertEquals(TransferAction.of(before.at(path).sha256(), before.at(path).sha256(), after.at(path).sha256()),
                    TransferAction.NOTHING, path + " would have been transferred again");
        }
    }
}
