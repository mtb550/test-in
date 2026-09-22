/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.git;

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitRefsTest {

    private static PendingChange diff(final Path relativePath) {
        return new PendingChange(ChangeSubject.TEST_CASE, "a case", "a test set", UUID.randomUUID().toString(),
                relativePath, DiffType.MODIFIED,
                TestCaseDto.builder().build(), List.of());
    }

    @Test
    public void everyFormGitCloneTakesIsAnAddress() {
        assertTrue(GitRefs.isRepositoryUrl("https://github.com/acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("ssh://git@github.com/acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("git@host:acme/repo.git"));
        assertTrue(GitRefs.isRepositoryUrl("http://localhost/x.git"), "plain http is a scheme git clone takes");
        assertTrue(GitRefs.isRepositoryUrl("git://host/x"), "the file's own rule used to drop this one");
        assertTrue(GitRefs.isRepositoryUrl("https://host/r.git?ref=main"), "a query is part of the address");
        assertTrue(GitRefs.isRepositoryUrl("  https://github.com/acme/repo.git  "), "surrounding space is trimmed");
    }

    @Test
    public void aProjectNameIsNotAnAddress() {
        assertFalse(GitRefs.isRepositoryUrl("NAFATH"));
        assertFalse(GitRefs.isRepositoryUrl("Checkout Regression"));
        assertFalse(GitRefs.isRepositoryUrl(""));
        assertFalse(GitRefs.isRepositoryUrl("   "));
    }

    @Test
    public void textWithCharactersNoAddressHasIsRefused() {
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r; rm -rf /"), "a semicolon is not in a clone address");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r a"), "nor is a space inside one");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/\"r\""), "nor a quote");
        assertFalse(GitRefs.isRepositoryUrl("https://x.com/r|whoami"), "nor a pipe");
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
    public void headBranchIsEmptyWhenTheRemoteDoesNotReportOne() {
        final String remoteShow = """
                * remote origin
                  Fetch URL: https://github.com/mtb550/test-in.git
                """;

        assertEquals(GitRefs.parseHeadBranch(remoteShow), "");
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
        assertEquals(GitRefs.chooseRemote(List.of()), "");
        assertEquals(GitRefs.chooseRemote(List.of("", "   ")), "");
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
    public void aLocalBranchWithASlashIsNotARemoteBranch() {
        assertFalse(GitRefs.isRemoteBranch("feature/login", List.of("feature/login", "main"), List.of("origin")));
    }

    @Test
    public void aRemoteBranchIsNamedAfterItsRemote() {
        assertTrue(GitRefs.isRemoteBranch("origin/main", List.of("main"), List.of("origin")));
        assertTrue(GitRefs.isRemoteBranch("origin/feature/login", List.of("main"), List.of("origin")));
        assertEquals(GitRefs.localNameOf("origin/feature/login"), "feature/login");
    }

    @Test
    public void aNameStartingWithNoRemoteIsNotARemoteBranch() {
        assertFalse(GitRefs.isRemoteBranch("feature/login", List.of("main"), List.of("origin")));
        assertFalse(GitRefs.isRemoteBranch("origin/main", List.of("main"), List.of()));
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

    @Test
    public void anUntrackedFileIsAnAddition() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("?? .tp"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().type(), DiffType.ADDED);
        assertEquals(entries.getFirst().path(), ".tp");
    }

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

    @Test
    public void aCopyIsOnlyTheNewFile() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("C  a.json -> b.json"));

        assertEquals(entries.size(), 1);
        assertEquals(entries.getFirst().path(), "b.json");
    }

    @Test
    public void aRenameThenDeletedIsTwoDeletions() {
        final List<GitRefs.StatusEntry> entries = GitRefs.parseStatus(List.of("RD a.json -> b.json"));

        assertEquals(entries.size(), 2);
        assertEquals(entries.getFirst().type(), DiffType.DELETED);
        assertEquals(entries.get(1).type(), DiffType.DELETED);
    }

    @Test
    public void aQuotedPathLosesItsQuotes() {
        assertEquals(GitRefs.parseStatus(List.of("?? \"Test Cases/login flow/a.json\"")).getFirst().path(),
                "Test Cases/login flow/a.json");
    }

    @Test
    public void aNonAsciiPathIsDecodedBackToItsName() {
        final String escaped = "?? \"Test Cases/\\330\\252\\330\\263\\330\\254\\331\\212\\331\\204/a.json\"";

        assertEquals(GitRefs.parseStatus(List.of(escaped)).getFirst().path(),
                "Test Cases/تسجيل/a.json");
    }

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

    @Test
    public void aRemoteWithNoBranchesNamesNoHeadBranch() {
        final String output = """
                * remote origin
                  Fetch URL: https://github.com/mtb550/testin-sync-check.git
                  Push  URL: https://github.com/mtb550/testin-sync-check.git
                  HEAD branch: (unknown)
                """;

        assertEquals(GitRefs.parseHeadBranch(output), "", "an empty remote has no branch to name");
    }

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
