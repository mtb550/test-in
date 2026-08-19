package org.testin.git;

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;

/**
 * The git naming and selection rules, exercised without an IDE or a repository -
 * which is what {@link GitRefs} was extracted for.
 */
public class GitRefsTest {

    private static PendingChange diff(final Path relativePath) {
        return new PendingChange(ChangeSubject.TEST_CASE, "a case", "a test set", UUID.randomUUID().toString(),
                relativePath, DiffType.MODIFIED,
                TestCaseDto.builder().build(), TestCaseDto.builder().build(), List.of());
    }

    @Test
    public void readsTheHeadBranchOutOfRemoteShowOutput() {
        final String remoteShow = """
                * remote origin
                  Fetch URL: https://github.com/mtb550/test-in.git
                  Push  URL: https://github.com/mtb550/test-in.git
                  HEAD branch: main
                  Remote branches:
                    main    tracked
                    develop tracked
                """;

        assertEquals(GitRefs.parseHeadBranch(remoteShow), "main");
    }

    @Test
    public void headBranchIsNullWhenTheRemoteDoesNotReportOne() {
        final String remoteShow = """
                * remote origin
                  Fetch URL: https://github.com/mtb550/test-in.git
                """;

        assertNull(GitRefs.parseHeadBranch(remoteShow));
    }

    @Test
    public void originWinsWhateverOrderTheRemotesArrive() {
        assertEquals(GitRefs.chooseRemote(List.of("upstream", "origin", "fork")), "origin");
    }

    @Test
    public void withoutOriginTheFirstRemoteIsUsed() {
        assertEquals(GitRefs.chooseRemote(List.of("upstream", "fork")), "upstream");
    }

    @Test
    public void remoteNamesAreTrimmedAndBlankLinesIgnored() {
        assertEquals(GitRefs.chooseRemote(List.of("", "  ", "  upstream  ")), "upstream");
    }

    @Test
    public void noRemotesMeansNoRemoteToSyncWith() {
        assertNull(GitRefs.chooseRemote(List.of()));
        assertNull(GitRefs.chooseRemote(List.of("", "   ")));
    }

    @Test
    public void remoteBranchNamesLoseTheirRemotePrefix() {
        assertEquals(GitRefs.localNameOf("origin/main"), "main");
    }

    @Test
    public void aLocalBranchNameIsAlreadyItsLocalName() {
        assertEquals(GitRefs.localNameOf("main"), "main");
    }

    @Test
    public void pathsAreForwardSlashedForGitWhateverThePlatformUses() {
        final Set<String> paths = GitRefs.repoRelativePaths(List.of(
                diff(Path.of("testCases", "login", "case-1.json"))));

        assertEquals(paths, Set.of("testCases/login/case-1.json"));
    }

    @Test
    public void theSameFileSelectedTwiceIsStagedOnce() {
        final Path shared = Path.of("testCases", "login", "case-1.json");

        final Set<String> paths = GitRefs.repoRelativePaths(List.of(
                diff(shared),
                diff(Path.of("testCases", "login", "case-2.json")),
                diff(shared)));

        assertEquals(List.copyOf(paths),
                List.of("testCases/login/case-1.json", "testCases/login/case-2.json"));
    }

    // ---------------------------------------------------------- git status
    //
    // The lines below are copied from a real `git status --porcelain -uall` run
    // against a freshly initialized Testin root, not invented. The review reads
    // this and nothing else, so a parsing mistake here is the review being
    // silently wrong about what changed.

    @Test
    public void anUntrackedFileIsAnAddition() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("?? .tp"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().type(), DiffType.ADDED);
        assertEquals(entries.getFirst().path(), ".tp");
    }

    /**
     * The case that broke the feature: a brand-new test case is untracked, so if
     * untracked did not count as added, the first commit of a new test set could
     * never be made from the plugin.
     */
    @Test
    public void everyNewTestCaseInANewTestSetIsReported() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of(
                "?? .tp",
                "?? \"Test Cases/.tcd\"",
                "?? \"Test Cases/rp/.ts\"",
                "?? \"Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json\"",
                "?? \"Test Cases/rp/84e8bf04-815d-4a48-81e2-3985b1e25c75.json\""));

        assertEquals(entries.size(), 5);
        assertEquals(entries.stream().filter(e -> e.path().endsWith(".json")).count(), 2);
        assertEquals(entries.get(3).path(), "Test Cases/rp/73ebd4d7-4a2c-4813-92d5-30aebe3a3670.json");
    }

    @Test
    public void aModifiedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitRefs.parseStatus(List.of(" M a.json")).getFirst().type(), DiffType.MODIFIED);
        assertEquals(GitRefs.parseStatus(List.of("M  a.json")).getFirst().type(), DiffType.MODIFIED);
        assertEquals(GitRefs.parseStatus(List.of("MM a.json")).getFirst().type(), DiffType.MODIFIED);
    }

    @Test
    public void aDeletedFileIsReportedWhicheverColumnCarriesIt() {
        assertEquals(GitRefs.parseStatus(List.of(" D a.json")).getFirst().type(), DiffType.DELETED);
        assertEquals(GitRefs.parseStatus(List.of("D  a.json")).getFirst().type(), DiffType.DELETED);
    }

    @Test
    public void aStagedAdditionIsAnAddition() {
        assertEquals(GitRefs.parseStatus(List.of("A  a.json")).getFirst().type(), DiffType.ADDED);
    }

    /**
     * A rename is two changes and both belong in the commit. Listing only the
     * new path leaves the old file behind for whoever pulls it, which is the
     * whole defect: they get the test case twice, under both names.
     */
    @Test
    public void aRenameIsBothTheDeletionAndTheAddition() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(
                List.of("R  \"Test Cases/old/a.json\" -> \"Test Cases/new/a.json\""));

        assertEquals(entries.size(), 2);

        assertEquals(entries.getFirst().type(), DiffType.DELETED);
        assertEquals(entries.getFirst().path(), "Test Cases/old/a.json");

        assertEquals(entries.get(1).type(), DiffType.ADDED);
        assertEquals(entries.get(1).path(), "Test Cases/new/a.json");
    }

    /**
     * A copy leaves its source exactly where it was, so the arrow means
     * something else entirely: one row, and no deletion.
     */
    @Test
    public void aCopyIsOnlyTheNewFile() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("C  a.json -> b.json"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().path(), "b.json");
    }

    /**
     * A rename whose new file was then deleted is a deletion of both sides -
     * nothing arrives under the new name, so nothing is added.
     */
    @Test
    public void aRenameThenDeletedIsTwoDeletions() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("RD a.json -> b.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().type(), DiffType.DELETED);
        assertEquals(entries.get(1).type(), DiffType.DELETED);
    }

    /**
     * Every test set with a space in its name arrives quoted, and Testin's own
     * fixed containers are called "Test Cases" and "Test Runs" - so this is not
     * an edge case, it is every repository.
     */
    @Test
    public void aQuotedPathLosesItsQuotes() {
        assertEquals(GitRefs.parseStatus(List.of("?? \"Test Cases/login flow/a.json\"")).getFirst().path(),
                "Test Cases/login flow/a.json");
    }

    /**
     * Git escapes non-ASCII bytes as octal, so a test set named in Arabic comes
     * back as escapes and has to be decoded as UTF-8 to match the file on disk.
     */
    @Test
    public void aNonAsciiPathIsDecodedBackToItsName() {
        // "تسجيل" - the UTF-8 bytes of the Arabic word, as git would escape them.
        final String escaped = "?? \"Test Cases/\\330\\252\\330\\263\\330\\254\\331\\212\\331\\204/a.json\"";

        assertEquals(GitRefs.parseStatus(List.of(escaped)).getFirst().path(),
                "Test Cases/\u062A\u0633\u062C\u064A\u0644/a.json");
    }

    /**
     * A commit has to carry the markers of every directory its test cases sit
     * under, so the directories a colleague pulls are recognisable as test sets.
     * The repository root is included as the empty string, because the test
     * project's own marker lives there.
     */
    @Test
    public void everyDirectoryAboveASelectedTestCaseIsFound() {
        assertEquals(GitRefs.ancestorDirectories(List.of(
                        "Test Cases/rp/a.json",
                        "Test Cases/rp/b.json",
                        "Test Cases/login flow/c.json")),
                Set.of("", "Test Cases", "Test Cases/rp", "Test Cases/login flow"));
    }

    @Test
    public void aFileAtTheRepositoryRootHasOnlyTheRoot() {
        assertEquals(GitRefs.ancestorDirectories(List.of("a.json")), Set.of(""));
    }

    /**
     * The line a remote with no commits prints. Taken literally it is a branch
     * called "(unknown)", which is what a first push tried to pull from -
     * "couldn't find remote ref (unknown)" - before the push was ever attempted.
     */
    @Test
    public void aRemoteWithNoBranchesNamesNoHeadBranch() {
        final String output = """
                * remote origin
                  Fetch URL: https://github.com/mtb550/testin-sync-check.git
                  Push  URL: https://github.com/mtb550/testin-sync-check.git
                  HEAD branch: (unknown)
                """;

        assertNull(GitRefs.parseHeadBranch(output), "an empty remote has no branch to name");
    }

    // ------------------------------------------------------------ branches

    /**
     * Taken from a real `git branch -a`. The symbolic ref line names no branch
     * of its own - checking it out detaches HEAD - so it must not sit in the
     * list looking like a third branch a tester can pick.
     */
    @Test
    public void theBranchListDropsTheMarkerAndTheSymbolicRef() {
        assertEquals(GitRefs.parseBranches(List.of(
                        "* main",
                        "  remotes/origin/HEAD -> origin/main",
                        "  remotes/origin/main")),
                List.of("main", "origin/main"));
    }

    @Test
    public void aRepositoryWithOneBranchListsOne() {
        assertEquals(GitRefs.parseBranches(List.of("* master")), List.of("master"));
    }

    @Test
    public void anEmptyBranchListIsNotAnError() {
        assertEquals(GitRefs.parseBranches(List.of()), List.of());
        assertEquals(GitRefs.parseBranches(List.of("", "   ")), List.of());
    }

    // ----------------------------------------------------------- conflicts

    /**
     * The seven codes Git uses for a path both sides touched. Getting this wrong
     * in either direction is bad: miss one and the tester is never offered the
     * abort, invent one and they are offered it on a clean pull.
     */
    @Test
    public void everyUnmergedCodeCountsAsAConflict() {
        for (final String code : List.of("DD", "AU", "UD", "UA", "DU", "AA", "UU")) {
            assertTrue(GitRefs.hasUnmergedPaths(List.of(code + " Test Cases/a.json")), code + " is a conflict");
        }
    }

    @Test
    public void ordinaryChangesAreNotConflicts() {
        assertFalse(GitRefs.hasUnmergedPaths(List.of(" M a.json", "?? b.json", "A  c.json", "D  d.json")));
        assertFalse(GitRefs.hasUnmergedPaths(List.of()));
    }

    @Test
    public void anIgnoredFileIsNotAChange() {
        assertEquals(GitRefs.parseStatus(List.of("!! build/output.json")), List.of());
    }

    @Test
    public void emptyAndTruncatedLinesAreSkipped() {
        assertEquals(GitRefs.parseStatus(List.of("", "  ", "??")), List.of());
    }
}
