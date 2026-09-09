package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * An edit Testin did not make is never ignored (#278).
 * <p>
 * The plugin ignores its own writes for five seconds, because otherwise every
 * save would re-read the project and rebuild the tree under the tester who
 * caused it. The window could not tell a second change from the first, so a
 * tester who edited the same file by hand inside those five seconds was ignored
 * along with it: their edit sat on disk, absent from the screen, until they
 * pressed Refresh.
 * <p>
 * What is pinned here is the property rather than the mechanism - <b>the answer
 * is about what the file says, not about when it changed</b> - so a later
 * implementation that keeps the promise passes and one that goes back to a bare
 * clock does not.
 */
public class OwnWritesTest {

    private static final byte @NotNull [] OURS = "{\"description\":\"Log in\"}".getBytes();
    private static final byte @NotNull [] THEIRS = "{\"description\":\"Log in as admin\"}".getBytes();

    @Test
    public void ourOwnSaveIsIgnored() {
        final @NotNull Path file = tempFile(OURS);

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(file);
            ours.wrote(file, OURS);

            assertTrue(ours.areOurs(file), "the plugin's own save must not rebuild the tree under the tester who caused it");
        } finally {
            delete(file);
        }
    }

    /**
     * The defect, as a test. Same path, same five seconds, different content.
     */
    @Test
    public void aHandEditInsideTheWindowIsNotIgnored() {
        final @NotNull Path file = tempFile(OURS);

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(file);
            ours.wrote(file, OURS);

            write(file, THEIRS);

            assertFalse(ours.areOurs(file),
                    "a tester edited this file after the plugin wrote it, inside the window - their edit must reach the screen");
        } finally {
            delete(file);
        }
    }

    /**
     * A write still running has nothing to compare against, and the event can
     * arrive while it is in flight - which is why the claim is made before the
     * write rather than after it.
     */
    @Test
    public void aWriteStillInFlightIsOurs() {
        final @NotNull Path file = tempFile(OURS);

        try {
            final @NotNull OwnWrites ours = new OwnWrites();
            ours.record(file);

            assertTrue(ours.areOurs(file), "the claim is made before the write, so an event during it is still ours");
        } finally {
            delete(file);
        }
    }

    @Test
    public void aFileNobodyClaimedIsNeverOurs() {
        final @NotNull Path file = tempFile(OURS);

        try {
            assertFalse(new OwnWrites().areOurs(file));
        } finally {
            delete(file);
        }
    }

    private static @NotNull Path tempFile(final byte @NotNull [] content) {
        try {
            final @NotNull Path file = Files.createTempFile("testin-ownwrites", ".json");
            Files.write(file, content);
            return file;
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a file to watch: " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final byte @NotNull [] content) {
        try {
            Files.write(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not stand in for the tester's edit: " + ex.getMessage(), ex);
        }
    }

    private static void delete(final @NotNull Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException ex) {
            // Litter, not a failed test - and reporting it as one would hide
            // whichever assertion actually failed.
            System.err.println("Could not clean up " + file + ": " + ex.getMessage());
        }
    }
}
