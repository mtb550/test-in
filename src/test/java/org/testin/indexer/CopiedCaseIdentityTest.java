package org.testin.indexer;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Which files a copy gives new ids to.
 * <p>
 * A copied test case has to become a test case of its own
 * (Rule-TREE-PANEL-051), and the sweep that does that reads the files by the
 * same rule the scan does: a {@code .json} directly inside a test set is a test
 * case (Rule-INTERNAL-011).
 * <p>
 * It used to ask instead whether the file name parsed as a UUID, which is how
 * Testin names the files it writes but not the only legal name. A case file
 * named by hand was indexed by the scan and passed over here, so copying its
 * test set left the copy carrying the original's id - two files, one case, and
 * editing either edited both (#288).
 */
public class CopiedCaseIdentityTest {

    private static final @NotNull Path TEST_SET = Path.of("root", "Test Cases", "Login");
    private static final @NotNull Path TEST_RUN = Path.of("root", "Test Runs", "Cycle 1");

    /**
     * Stands in for the marker on disk: these directories are test sets and
     * nothing else is.
     */
    private static final @NotNull Predicate<Path> IS_TEST_SET = Set.of(TEST_SET)::contains;

    @Test
    public void aCaseTestinNamedIsACaseFile() {
        assertTrue(ProjectIndexer.isCaseFile(TEST_SET.resolve("3f2b9c14-0d5e-4a71-9c33-8e1f4b2a7d60.json"), IS_TEST_SET));
    }

    @Test
    public void aCaseTheTesterNamedIsACaseFileToo() {
        assertTrue(ProjectIndexer.isCaseFile(TEST_SET.resolve("login.json"), IS_TEST_SET),
                "a hand-named case is still a test case, so a copy of it has to get an id of its own");
    }

    @Test
    public void aRunsOwnFileIsNotACaseFile() {
        assertFalse(ProjectIndexer.isCaseFile(TEST_RUN.resolve("Cycle 1.json"), IS_TEST_SET),
                "a run's file is named for its folder and must keep the case ids it executed");
    }

    @Test
    public void aMarkerIsNotACaseFile() {
        assertFalse(ProjectIndexer.isCaseFile(TEST_SET.resolve(".ts"), IS_TEST_SET));
    }

    @Test
    public void jsonOutsideATestSetIsNotACaseFile() {
        assertFalse(ProjectIndexer.isCaseFile(Path.of("root", "Test Cases", "notes.json"), IS_TEST_SET));
    }
}
