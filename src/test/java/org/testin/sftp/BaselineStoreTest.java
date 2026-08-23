package org.testin.sftp;

import org.testin.util.Mapper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * What the sync remembers between runs (#94).
 * <p>
 * The baseline is the ancestor a three-way merge reads, so the two things that
 * matter are that it survives a round trip exactly, and that when it cannot be
 * read it says so by being empty rather than by being half of itself. A
 * half-read baseline would claim files were agreed that never arrived, and the
 * next sync would take that as the server having deleted them.
 */
public class BaselineStoreTest {

    private Path directory;
    private Mapper mapper;

    private static Mapper mapper() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build a Mapper for the test", ex);
        }
    }

    @BeforeMethod
    public void createDirectory() {
        try {
            directory = Files.createTempDirectory("testin-baseline");
            mapper = mapper();
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeDirectory() {
        try {
            if (directory == null || !Files.exists(directory)) return;

            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void whatIsStoredIsWhatComesBack() {
        final Baseline stored = new Baseline(Map.of(
                "Test Cases/pkg1/Login/6197ec6e.json", "{\"description\":\"Sign in\"}",
                "Test Cases/pkg1/.tsp", "{\"order\":4}"));

        final Path file = directory.resolve("one.json.gz");
        assertTrue(BaselineStore.write(mapper, file, stored));

        final Baseline read = BaselineStore.read(mapper, file);
        assertEquals(read.contents(), stored.contents());
        assertEquals(read.at("Test Cases/pkg1/Login/6197ec6e.json"), "{\"description\":\"Sign in\"}");
    }

    @Test
    public void aFileThatWasNeverTransferredReadsAsEmptyRatherThanMissing() {
        final Baseline stored = new Baseline(Map.of("a.json", "{}"));

        assertEquals(stored.at("never-seen.json"), "",
                "an empty ancestor makes a three-way merge treat both sides as additions, which they are");
    }

    @Test
    public void nothingStoredYetIsNotAnError() {
        assertSame(BaselineStore.read(mapper, directory.resolve("has-never-been-written.json.gz")), Baseline.EMPTY);
    }

    @Test
    public void anUnreadableBaselineAnswersEmptyRatherThanHalfOfItself() {
        try {
            final Path file = directory.resolve("corrupt.json.gz");
            Files.write(file, "this is not gzip".getBytes(StandardCharsets.UTF_8));

            assertSame(BaselineStore.read(mapper, file), Baseline.EMPTY,
                    "every file then looks new to both sides, so the next sync asks instead of deleting");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void nothingIsLeftBehindWhenAWriteFails() {
        try {
            // A directory with something in it where the file should be. An empty
            // one is not enough: a move that replaces an existing entry removes an
            // empty directory and succeeds, which is the platform behaving sensibly
            // and not the failure this is about.
            final Path taken = directory.resolve("taken.json.gz");
            Files.createDirectories(taken);
            Files.writeString(taken.resolve("occupied"), "in the way");

            assertFalse(BaselineStore.write(mapper, taken, new Baseline(Map.of("a.json", "{}"))),
                    "a baseline that could not be stored must say so");
            assertFalse(Files.exists(directory.resolve("taken.json.gz.part")),
                    "a half-written baseline claims files were agreed that never arrived");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void itIsStoredOutsideTheProjectItDescribes() {
        final Path project = Path.of("C:", "Users", "mtb", "Downloads", "Testin", "test-01");
        final Path file = BaselineStore.fileFor(project);

        assertFalse(file.startsWith(project), "a baseline inside the tree would be committed and indexed");
        assertTrue(file.toString().endsWith(".json.gz"));
        assertTrue(file.getFileName().toString().startsWith("test-01-"),
                "the folder name carries the meaning so the directory can be read by a human");
    }

    @Test
    public void twoProjectsWithTheSameFolderNameGetTheirOwn() {
        final Path one = Path.of("C:", "work", "alpha", "test-01");
        final Path two = Path.of("C:", "work", "beta", "test-01");

        assertFalse(BaselineStore.fileFor(one).equals(BaselineStore.fileFor(two)),
                "keyed on the whole path, not the folder name");
    }

    @Test
    public void aBaselineKnowsItsOwnHashes() {
        final Baseline baseline = new Baseline(Map.of("a.json", "{\"x\":1}"));
        final Manifest manifest = baseline.manifest();

        assertEquals(manifest.at("a.json").sha256(), Manifest.sha256("{\"x\":1}"));
        assertTrue(manifest.at("b.json").isAbsent());
    }
}
