package org.testin.indexer;

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * What the file watcher lets through (#20).
 * <p>
 * This decision runs on every file event in the IDE - a build writing class
 * files, the platform's own indexes, every other plugin - so it has to be
 * arithmetic on paths and it has to answer "no" for almost everything. Both
 * halves are worth pinning: letting too much through re-reads test projects for
 * changes that were never test data, and letting too little through is the bug
 * the watcher exists to fix, silently.
 * <p>
 * The one that would cost most is Git's own directory. A test project is itself
 * a repository, so {@code .git} sits inside the project rather than beside it,
 * and a pull rewrites HEAD, FETCH_HEAD, the index and the logs - which without
 * this would look like the project's test data changing, over and over, for the
 * length of the pull.
 */
public class WatchedPathTest {

    private static final Path ROOT = Path.of("C:", "Users", "mtb", "Testin");

    private static final Path PROJECT = ROOT.resolve("test-01");

    private static Optional<Path> of(final Path changed) {
        return WatchedPath.testProjectOf(changed, ROOT);
    }

    @Test
    public void aTestCaseThatChangedNamesItsProject() {
        assertEquals(of(PROJECT.resolve("Test Cases/Login/6197ec6e.json")), Optional.of(PROJECT),
                "the project is the unit the indexer re-reads, not the file");
    }

    @Test
    public void aMarkerNamesItsProjectToo() {
        assertEquals(of(PROJECT.resolve("Test Cases/Login/.ts")), Optional.of(PROJECT));
        assertEquals(of(PROJECT.resolve(".tp")), Optional.of(PROJECT));
    }

    @Test
    public void everyFileOfOneProjectAnswersTheSameProject() {
        assertEquals(of(PROJECT.resolve("Test Runs/Cycle 1/run.json")), of(PROJECT.resolve(".tp")),
                "forty files changed in one project have to collapse to one scan");
    }

    @Test
    public void aSecondProjectIsItsOwnAnswer() {
        assertEquals(of(ROOT.resolve("test-02/.tp")), Optional.of(ROOT.resolve("test-02")));
    }

    @Test
    public void gitsOwnFilesAreNotTestData() {
        assertEquals(of(PROJECT.resolve(".git/HEAD")), Optional.empty(),
                "a pull rewrites these constantly and none of it is test data");
        assertEquals(of(PROJECT.resolve(".git/index")), Optional.empty());
        assertEquals(of(PROJECT.resolve(".git/refs/heads/main")), Optional.empty());
    }

    @Test
    public void aFolderCalledGitDeeperInIsStillGits() {
        assertEquals(of(PROJECT.resolve("Test Cases/.git/config")), Optional.empty(),
                "checking only the top segment would let every pull through");
    }

    @Test
    public void anythingOutsideTheTestinRootIsIgnored() {
        assertEquals(of(Path.of("C:", "Users", "mtb", "IdeaProjects", "testin", "build", "Foo.class")),
                Optional.empty(), "this is nearly every event the listener will ever see");
        assertEquals(of(Path.of("C:", "Users", "mtb", "TestinOther", "test-01", ".tp")), Optional.empty(),
                "a sibling folder whose name merely starts the same way is not inside the root");
    }

    @Test
    public void theRootItselfIsAFolderOfProjectsAndNotOne() {
        assertEquals(of(ROOT), Optional.empty());
    }

    @Test
    public void anUnconfiguredRootWatchesNothing() {
        assertTrue(WatchedPath.testProjectOf(PROJECT.resolve(".tp"), Path.of("")).isEmpty(),
                "there is nothing to watch before a tester has set a Testin folder");
    }
}
