package org.testin.sftp;

import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A project packed into one file and read back (#94).
 * <p>
 * The bundle exists because SFTP charges by the file rather than by the byte,
 * so the properties worth pinning are that nothing is lost or altered on the way
 * through, and that two runs over the same project produce the same bytes.
 */
public class ProjectBundleTest {

    private static final Path PROJECT = Path.of("C:", "Users", "mtb", "Downloads", "Testin", "test-01");

    private static Map<String, byte[]> sampleProject() {
        final Map<String, byte[]> files = new TreeMap<>();
        files.put(".tp", "{\"status\":\"ACTIVE\"}".getBytes(StandardCharsets.UTF_8));
        files.put("Test Cases/.tcd", "{}".getBytes(StandardCharsets.UTF_8));
        files.put("Test Cases/pkg1/.tsp", "{\"order\":4}".getBytes(StandardCharsets.UTF_8));
        files.put("Test Cases/pkg1/Login/6197ec6e.json",
                "{\"description\":\"Sign in\"}".getBytes(StandardCharsets.UTF_8));

        return files;
    }

    @Test
    public void everythingComesBackExactlyAsItWentIn() {
        final Map<String, byte[]> before = sampleProject();
        final Map<String, byte[]> after = ProjectBundle.unpack(ProjectBundle.pack(before));

        assertEquals(after.keySet(), before.keySet());
        before.forEach((path, content) -> assertEquals(after.get(path), content, path));
    }

    @Test
    public void markersAndSpacesSurvive() {
        final Map<String, byte[]> after = ProjectBundle.unpack(ProjectBundle.pack(sampleProject()));

        assertTrue(after.containsKey(".tp"), "a marker is a dotfile and the indexer needs every one");
        assertTrue(after.containsKey("Test Cases/pkg1/Login/6197ec6e.json"), "a folder name with a space");
    }

    @Test
    public void anEmptyProjectIsAnEmptyBundleAndNotAFailure() {
        assertEquals(ProjectBundle.unpack(ProjectBundle.pack(Map.of())), Map.of());
    }

    /**
     * Two runs over the same project give the same bytes.
     * <p>
     * Otherwise every rebuild would look like a change to anything comparing
     * bundles, which is the opposite of what a manifest is for.
     */
    @Test
    public void packingTwiceGivesTheSameBytes() {
        assertEquals(ProjectBundle.pack(sampleProject()), ProjectBundle.pack(sampleProject()));
    }

    /**
     * A path longer than tar's ancient 100-character limit is not truncated.
     * <p>
     * A truncated path is two test cases colliding on one name, which would lose
     * one of them silently.
     */
    @Test
    public void aVeryLongPathIsNotTruncated() {
        final String deep = "Test Cases/" + "a-rather-long-package-name/".repeat(6)
                + "6197ec6e-a0ce-4396-9117-693990efce47.json";
        assertTrue(deep.length() > 100, "the point of this test is a path over the limit: " + deep.length());

        final Map<String, byte[]> after =
                ProjectBundle.unpack(ProjectBundle.pack(Map.of(deep, "{}".getBytes(StandardCharsets.UTF_8))));

        assertEquals(after.keySet(), java.util.Set.of(deep));
    }

    /**
     * A real project packs and unpacks whole, and says how big it is.
     * <p>
     * Skipped where that project is not on the machine. The bundle is not
     * compressed - the saving that made this design win is one transfer instead
     * of 2,246, which is true whatever the bytes are - so what matters here is
     * that nothing is lost, not how small it got.
     */
    @Test
    public void aRealProjectPacksAndUnpacksWhole() throws IOException {
        if (!Files.isDirectory(PROJECT)) throw new SkipException("No test project at " + PROJECT);

        final Map<String, byte[]> files = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(PROJECT)) {
            final List<Path> regular = paths.filter(Files::isRegularFile)
                    .filter(p -> !PROJECT.relativize(p).toString().replace('\\', '/').startsWith(".git/"))
                    .toList();

            for (final Path path : regular) {
                files.put(PROJECT.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
            }
        }

        final long raw = files.values().stream().mapToLong(content -> content.length).sum();
        final byte[] bundle = ProjectBundle.pack(files);

        System.out.println("[bundle] " + files.size() + " files, " + raw + " bytes of content -> "
                + bundle.length + " bytes packed");

        assertEquals(ProjectBundle.unpack(bundle).size(), files.size(), "every file survives the round trip");
        assertTrue(bundle.length >= raw, "a plain tar carries the bytes plus a header for each file");
    }
}
