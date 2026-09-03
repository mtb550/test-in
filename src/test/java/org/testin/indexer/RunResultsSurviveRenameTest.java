package org.testin.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Renaming a test run keeps its results (#177).
 * <p>
 * The results used to be named after the folder holding them -
 * {@code Cycle-1/Cycle-1.json} - which made a run the only node in the tree whose
 * contents were named after it, and so the only node a rename could empty. It
 * did: the write derived that name and the scan's read derived it again, and
 * neither told the rename. Renaming a cycle moved the folder, left the results
 * behind under the old name, and the next index found nothing where a whole
 * cycle had been.
 * <p>
 * What actually fixes that is not the rename learning to carry the file, but the
 * name having nothing to keep in step with - so what is pinned here is the
 * property rather than the operation: <b>the file a run's results live in is the
 * same whatever the folder is called.</b> That one sentence is the bug and the
 * fix. It fails against the old implementation and passes against this one, which
 * is what makes it the regression test.
 * <p>
 * Deliberately not a test of {@code ProjectIndexer.renameNode} itself. That needs
 * the VFS inside a write action, this repository has no platform test harness
 * (#108), and the rename is now three lines that know nothing about test runs -
 * the part that was ever specific to a run is the naming, and the naming is here.
 * The folder rename below is therefore a real one on disk, standing in for the
 * VFS operation that performs it in the IDE.
 */
public class RunResultsSurviveRenameTest {

    /**
     * Enough of a run to be worth losing: one recorded verdict, which is what a
     * tester would not get back.
     */
    private static final @NotNull String A_RUN = """
            {
              "results" : [ {
                "id" : "11111111-1111-4111-8111-111111111101",
                "status" : "PASSED",
                "actualResult" : "Signed in and reached the dashboard."
              } ]
            }""";

    @Test
    public void theResultsFileNameDoesNotDependOnTheFolderName() {
        final @NotNull Path cycle = Path.of("C:", "Testin", "Demo", "Test Runs", "Cycle-1");
        final @NotNull Path renamed = cycle.resolveSibling("Smoke, third attempt");

        assertEquals(TestRunDirectoryDto.resultsFile(cycle).getFileName(),
                TestRunDirectoryDto.resultsFile(renamed).getFileName(),
                "A run's results are found by a name that does not move when the folder is renamed."
                        + " The moment those two names differ, renaming a run empties it.");
    }

    @Test
    public void theResultsLiveInsideTheFolderTheyBelongTo() {
        final @NotNull Path cycle = Path.of("C:", "Testin", "Demo", "Test Runs", "Cycle-1");

        assertEquals(TestRunDirectoryDto.resultsFile(cycle).getParent(), cycle,
                "The results sit inside the run's own folder, which is why moving the folder to another"
                        + " package carries them along with no help from the move");
    }

    /**
     * The whole defect, end to end, on a real directory: write a run, rename its
     * folder the way a tester does, and read the results back from where the scan
     * would look for them.
     */
    @Test
    public void renamingTheFolderKeepsTheResultsWhereTheScanLooks() {
        final @NotNull Path root = tempRoot();

        try {
            final @NotNull Path cycle = createRun(root.resolve("Cycle-1"));
            final @NotNull Path renamed = move(cycle, root.resolve("Cycle-1 (rerun)"));

            final @NotNull Path results = TestRunDirectoryDto.resultsFile(renamed);
            assertTrue(Files.exists(results),
                    "After a rename the scan looks for " + results.getFileName() + " and it has to be there,"
                            + " or every verdict in the run is gone at the next index");

            final @NotNull TestRunDto run = read(results);
            assertFalse(run.getResults().isEmpty(), "The rename kept the file and lost what was in it");
            assertEquals(run.getResults().getFirst().getStatus().name(), "PASSED",
                    "The verdict the tester recorded before the rename is the thing this issue was about");

            assertTrue(Files.exists(renamed.resolve(".tr")), "The marker moved with the folder, as it always did");
        } finally {
            deleteTree(root);
        }
    }

    /**
     * The wipe, asserted rather than assumed.
     * <p>
     * A run written by an older build carries {@code <folder>.json} and nothing
     * reads it - decided on 2026-09-03, following the project's habit of deleting
     * old test data rather than migrating it. That is a choice, and a choice that
     * nothing records is indistinguishable from an oversight the next time
     * somebody wonders why an upgraded run looks empty.
     */
    @Test
    public void aRunWrittenByAnOlderBuildIsNotRead() {
        final @NotNull Path root = tempRoot();

        try {
            final @NotNull Path cycle = root.resolve("Cycle-1");
            write(cycle.resolve(cycle.getFileName() + ".json"), A_RUN);

            assertFalse(Files.exists(TestRunDirectoryDto.resultsFile(cycle)),
                    "The old format is not read. A run from before this change shows no results, and its file"
                            + " stays on disk as litter - expected, not a defect");
        } finally {
            deleteTree(root);
        }
    }

    private static @NotNull Path createRun(final @NotNull Path folder) {
        write(folder.resolve(".tr"), "{}");
        write(TestRunDirectoryDto.resultsFile(folder), A_RUN);
        return folder;
    }

    private static @NotNull Path tempRoot() {
        try {
            return Files.createTempDirectory("testin-rename");
        } catch (final IOException ex) {
            throw new AssertionError("Could not create a temporary directory to rename in: " + ex.getMessage(), ex);
        }
    }

    private static void write(final @NotNull Path file, final @NotNull String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (final IOException ex) {
            throw new AssertionError("Could not write " + file + ": " + ex.getMessage(), ex);
        }
    }

    private static @NotNull Path move(final @NotNull Path from, final @NotNull Path to) {
        try {
            return Files.move(from, to);
        } catch (final IOException ex) {
            throw new AssertionError("Could not rename " + from + " to " + to + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Read the way the indexer reads it, so a format change fails here too rather
     * than only in the sandbox.
     */
    private static @NotNull TestRunDto read(final @NotNull Path file) {
        try {
            return new ObjectMapper().registerModule(new JavaTimeModule()).readValue(file.toFile(), TestRunDto.class);
        } catch (final IOException ex) {
            throw new AssertionError("The run at " + file + " no longer parses: " + ex.getMessage(), ex);
        }
    }

    private static void deleteTree(final @NotNull Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (final IOException ex) {
            // A leftover temporary directory is litter, not a failed test - and
            // reporting it as one would hide whichever assertion actually failed.
            System.err.println("Could not clean up " + root + ": " + ex.getMessage());
        }
    }
}
