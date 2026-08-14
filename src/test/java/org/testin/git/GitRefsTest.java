package org.testin.git;

import org.testin.mappers.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * The git naming and selection rules, exercised without an IDE or a repository -
 * which is what {@link GitRefs} was extracted for.
 */
public class GitRefsTest {

    private static TestCaseDiff diff(final Path relativePath) {
        return new TestCaseDiff(UUID.randomUUID().toString(), relativePath, DiffType.MODIFIED,
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
}
