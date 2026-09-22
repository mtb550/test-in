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

import org.testin.TempTree;
import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.RealMapper;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitWorkflowTest {

    private static final int WINDOWS_COMMAND_LINE_LIMIT = 32767;
    private Path remote;

    private Path work;

    private static Optional<String> git(final Path directory, final String... arguments) {
        final List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        try {
            final Process process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.waitFor() == 0 ? Optional.of(output) : Optional.empty();

        } catch (final IOException | InterruptedException ex) {
            return Optional.empty();
        }
    }

    private static String mustGit(final Path directory, final String... arguments) {
        return git(directory, arguments).orElseThrow(() -> new AssertionError(
                "git " + String.join(" ", arguments) + " failed in " + directory));
    }

    @BeforeMethod
    public void createRepositories() {
        try {
            if (git(Path.of("."), "--version").isEmpty()) {
                throw new SkipException("Git is not on the PATH, so the workflow cannot be exercised");
            }

            final Path base = Files.createTempDirectory("testin-workflow");
            remote = base.resolve("remote.git");
            work = base.resolve("work");
            Files.createDirectories(remote);
            Files.createDirectories(work);

            mustGit(remote, "init", "--bare", "--initial-branch=main");
            mustGit(work, "init", "--initial-branch=main");

            mustGit(work, "config", "user.name", "Testin Test");
            mustGit(work, "config", "user.email", "testin@example.invalid");
            mustGit(work, "remote", "add", "origin", remote.toUri().toString());
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositories() {
        if (remote != null) TempTree.delete(remote.getParent());
    }

    @Test
    public void aCommitTooLargeForTheCommandLineStillLands() {
        try {
            mustGit(work, "commit", "--allow-empty", "-m", "root");

            final Path set = work.resolve("test-01").resolve("Test Cases").resolve("pkg1").resolve("Login");
            Files.createDirectories(set);

            final Set<String> paths = new LinkedHashSet<>();
            for (int i = 0; i < 1200; i++) {
                final String name = UUID.randomUUID() + ".tc";
                Files.writeString(set.resolve(name), "{}");
                paths.add("test-01/Test Cases/pkg1/Login/" + name);
            }

            final int asArguments = paths.stream().mapToInt(path -> path.length() + 3).sum();
            assertTrue(asArguments > WINDOWS_COMMAND_LINE_LIMIT,
                    "the point of this test is a list that cannot be passed as arguments, and this one is only "
                            + asArguments + " characters");

            final Path pathspec = Files.createTempFile("testin-pathspec", ".lst");
            Files.write(pathspec, GitCommandRunner.pathspecBytes(paths));

            mustGit(work, "add", "--pathspec-from-file=" + pathspec, "--pathspec-file-nul");
            mustGit(work, "commit", "--only", "-m", "imported 1200 cases",
                    "--pathspec-from-file=" + pathspec, "--pathspec-file-nul");

            final long committed = mustGit(work, "show", "--name-only", "--pretty=format:", "HEAD")
                    .lines().filter(line -> !line.isBlank()).count();

            assertEquals(committed, 1200, "every selected case belongs in the commit");
            assertTrue(mustGit(work, "status", "--porcelain").isBlank(), "nothing should be left behind");

            Files.deleteIfExists(pathspec);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private TestCaseDto testCase(final String description) {
        return TestCaseDto.builder()
                .description(description)
                .expectedResult("the account dashboard opens")
                .steps(new ArrayList<>(List.of("open the app", "sign in")))
                .priority(Priority.HIGH)
                .module("authentication")
                .build();
    }

    private void write(final Path root, final String relativePath, final Object content) {

        try {
            final Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent() == null ? root : file.getParent());
            Files.writeString(file, content instanceof String text ? text : RealMapper.build().writeValueAsString(content),
                    StandardCharsets.UTF_8);

        } catch (final IOException ex) {

            throw new AssertionError(ex);

        }

    }

    private List<TestCaseDto> writeTestProject() {
        write(work, ".tp", "{\"status\":\"ACTIVE\"}");
        write(work, "Test Cases/.tcd", "{}");
        write(work, "Test Runs/.trd", "{}");
        write(work, "Test Cases/login flow/.ts", "{}");

        final List<TestCaseDto> testCases = List.of(
                testCase("a registered user signs in"),
                testCase("a wrong password is refused"));

        TestCaseOrder.rankAll(testCases);

        for (final TestCaseDto testCase : testCases) {
            write(work, "Test Cases/login flow/" + testCase.getId() + ".tc", testCase);
        }
        return testCases;
    }

    private List<PendingChange> review() {
        final List<String> status = mustGit(work, "status", "--porcelain", "-uall")
                .lines().filter(line -> !line.isBlank()).toList();

        return GitDiffProcessor.toDiffs(status, work, RealMapper.build(),
                path -> git(work, "show", "HEAD:" + path).orElse(""),
                id -> Optional.empty());
    }

    private Set<String> stagedFor(final List<PendingChange> review) {
        final Set<String> paths = new LinkedHashSet<>(GitRefs.repoRelativePaths(review));
        paths.addAll(GitCommits.markersAlongside(work, paths));
        return paths;
    }

    private void commit(final Set<String> paths, final String message) {
        final Set<String> stageable = GitCommits.stageable(work, paths);

        if (!stageable.isEmpty()) {
            final List<String> add = new ArrayList<>(List.of("add", "--"));
            add.addAll(stageable);
            mustGit(work, add.toArray(String[]::new));
        }

        final List<String> commit = new ArrayList<>(List.of("commit", "--only", "-m", message, "--"));
        commit.addAll(paths);
        mustGit(work, commit.toArray(String[]::new));
    }

    private Path cloneAsColleague() {

        final Path colleague = remote.getParent().resolve("colleague");
        mustGit(remote.getParent(), "clone", remote.toUri().toString(), colleague.toString());
        return colleague;

    }

    @Test
    public void aNewTestProjectIsReviewedCommittedAndPushed() {
        final List<TestCaseDto> testCases = writeTestProject();

        final List<PendingChange> pending = review();
        assertEquals(pending.stream().filter(change -> change.subject() == ChangeSubject.TEST_CASE).count(), 2,
                "both new test cases are in the review");
        assertTrue(pending.stream().anyMatch(change -> change.subject() == ChangeSubject.MARKER),
                "and so are the markers that make their directories nodes");
        assertTrue(pending.stream().allMatch(diff -> diff.type() == DiffType.ADDED));

        commit(stagedFor(pending), "the first commit");
        assertTrue(git(work, "push", "-u", "origin", "main").isPresent(), "the push to an empty remote succeeded");

        final Path colleague = cloneAsColleague();
        for (final TestCaseDto testCase : testCases) {
            assertTrue(Files.exists(colleague.resolve("Test Cases/login flow/" + testCase.getId() + ".tc")),
                    "the colleague received " + testCase.getDescription());
        }
    }

    @Test
    public void whatTheColleagueClonesIsAUsableTestProject() {
        writeTestProject();

        commit(stagedFor(review()), "the first commit");
        mustGit(work, "push", "-u", "origin", "main");

        final Path colleague = cloneAsColleague();

        assertTrue(Files.exists(colleague.resolve(".tp")), "the test project marker travelled");
        assertTrue(Files.exists(colleague.resolve("Test Cases/.tcd")), "the test cases container marker travelled");
        assertTrue(Files.exists(colleague.resolve("Test Cases/login flow/.ts")),
                "the test set marker travelled - without it the cases are in a directory nothing recognises");
    }

    @Test
    public void onlyTheMarkersAboveTheSelectedTestCasesTravel() {
        writeTestProject();

        final Set<String> staged = stagedFor(review().stream()
                .filter(change -> change.subject() == ChangeSubject.TEST_CASE)
                .toList());

        assertTrue(staged.contains(".tp"));
        assertTrue(staged.contains("Test Cases/.tcd"));
        assertTrue(staged.contains("Test Cases/login flow/.ts"));
        assertFalse(staged.contains("Test Runs/.trd"),
                "no test case sits under Test Runs, so its marker is not part of this commit");
    }

    @Test
    public void editingATestCaseShowsExactlyWhatChanged() {
        final List<TestCaseDto> testCases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        assertEquals(review(), List.of(), "nothing is pending straight after a commit");

        final TestCaseDto edited = testCases.getFirst().setModule("payments");
        write(work, "Test Cases/login flow/" + edited.getId() + ".tc", edited);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().type(), DiffType.MODIFIED);
        assertEquals(pending.getFirst().fieldChanges().size(), 1, "one field moved, so one row");
        assertEquals(pending.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_MODULE);
        assertEquals(pending.getFirst().fieldChanges().getFirst().oldValue(), "authentication");
        assertEquals(pending.getFirst().fieldChanges().getFirst().newValue(), "payments");
    }

    @Test
    public void addingATestCaseToACommittedTestSetIsReviewedAsAnAddition() {
        writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final TestCaseDto extra = testCase("a locked account cannot sign in");
        write(work, "Test Cases/login flow/" + extra.getId() + ".tc", extra);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().type(), DiffType.ADDED);
        assertEquals(pending.getFirst().name(), "a locked account cannot sign in");
    }

    @Test
    public void deletingATestCaseIsReviewedFromWhatWasCommitted() {
        try {
            final List<TestCaseDto> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");

            Files.delete(work.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".tc"));

            final List<PendingChange> pending = review();

            assertEquals(pending.size(), 1);
            assertEquals(pending.getFirst().type(), DiffType.DELETED);
            assertEquals(pending.getFirst().name(), "a registered user signs in");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aRenameStagedElsewhereCommitsBothSides() {
        final List<TestCaseDto> testCases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final String file = testCases.getFirst().getId() + ".tc";
        mustGit(work, "mv", "Test Cases/login flow/" + file, "Test Cases/" + file);

        final List<PendingChange> pending = review();
        assertEquals(pending.stream().filter(change -> change.type() == DiffType.DELETED).count(), 1);
        assertEquals(pending.stream().filter(change -> change.type() == DiffType.ADDED).count(), 1);

        commit(stagedFor(pending), "the case moved out of the test set");

        final List<String> committed = mustGit(work, "ls-tree", "-r", "--name-only", "HEAD")
                .lines().filter(line -> line.endsWith(file)).toList();

        assertEquals(committed.size(), 1);
        assertEquals(committed.getFirst(), "Test Cases/" + file);
        assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
    }

    @Test
    public void aConflictedTestCaseIsMergedFieldByFieldAndTheRebaseFinishes() {
        try {
            final List<TestCaseDto> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final String relativePath = "Test Cases/login flow/" + testCases.getFirst().getId() + ".tc";

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve(relativePath);
            final TestCaseDto theirs = RealMapper.build().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(theirCopy, RealMapper.build().writeValueAsString(
                            theirs.setExpectedResult("the dashboard opens within two seconds").setUpdatedBy("colleague")),
                    StandardCharsets.UTF_8);
            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            final Path myCopy = work.resolve(relativePath);
            final TestCaseDto mine = RealMapper.build().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(myCopy, RealMapper.build().writeValueAsString(
                            mine.setDescription("a registered user signs in with a valid password").setUpdatedBy("muteb")),
                    StandardCharsets.UTF_8);
            commit(stagedFor(review()), "reworded the description");

            assertTrue(git(work, "pull", "--rebase", "--autostash", "origin", "main").isEmpty(),
                    "the pull is expected to stop on the conflict");

            final List<String> conflicting = GitRefs.unmergedPaths(
                    mustGit(work, "status", "--porcelain", "-uall").lines().filter(line -> !line.isBlank()).toList());
            assertEquals(conflicting, List.of(relativePath));

            final String base = mustGit(work, "show", ":1:" + relativePath);
            final String remote = mustGit(work, "show", ":2:" + relativePath);
            final String replayed = mustGit(work, "show", ":3:" + relativePath);

            final Merge merge = TestCaseMerge.of(RealMapper.build(), base, replayed, remote);
            assertTrue(merge.isSettled(), "different fields are not a disagreement");

            Files.writeString(myCopy, merge.merged().toPrettyString(), StandardCharsets.UTF_8);
            mustGit(work, "add", "--", relativePath);
            mustGit(work, "-c", "core.editor=true", "rebase", "--continue");

            final TestCaseDto merged = RealMapper.build().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            assertEquals(merged.getDescription(), "a registered user signs in with a valid password");
            assertEquals(merged.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
            assertEquals(review(), List.of(), "a resolved rebase leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void twoTestersAddingTestCasesToOneSetDoNotConflictAtAll() {
        try {
            writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final TestCaseDto theirNewTestCase = testCase("a locked account cannot sign in").setOrder("s");
            write(colleague, "Test Cases/login flow/" + theirNewTestCase.getId() + ".tc", theirNewTestCase);

            mustGit(colleague, "add", "-A");
            mustGit(colleague, "commit", "-m", "added the locked account case");
            mustGit(colleague, "push", "origin", "main");

            final TestCaseDto myNewTestCase = testCase("a signed-in user signs out").setOrder("s");
            write(work, "Test Cases/login flow/" + myNewTestCase.getId() + ".tc", myNewTestCase);
            commit(stagedFor(review()), "added the sign out case");

            assertTrue(git(work, "pull", "--rebase", "--autostash", "origin", "main").isPresent(),
                    "two appended cases touch two files and merge on their own");

            final List<TestCaseDto> after = new ArrayList<>();
            try (Stream<Path> files = Files.list(work.resolve("Test Cases/login flow"))) {
                for (final Path file : files.filter(f -> f.getFileName().toString().endsWith(".tc")).sorted().toList()) {
                    after.add(RealMapper.build().readValue(Files.readString(file, StandardCharsets.UTF_8), TestCaseDto.class));
                }
            }

            assertEquals(after.size(), 4, "both testers keep their case");

            final List<TestCaseDto> ordered = TestCaseOrder.ordered(after);
            assertEquals(ordered, TestCaseOrder.ordered(new ArrayList<>(after.reversed())),
                    "the order does not depend on what order the files were read in");
            assertTrue(ordered.stream().anyMatch(tc -> tc.getId().equals(theirNewTestCase.getId())));
            assertTrue(ordered.stream().anyMatch(tc -> tc.getId().equals(myNewTestCase.getId())));
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void aColleaguesChangeArrivesOnAPull() {
        try {
            final List<TestCaseDto> testCases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".tc");
            final TestCaseDto theirs = RealMapper.build().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(theirCopy, RealMapper.build().writeValueAsString(theirs.setExpectedResult("the dashboard opens within two seconds")),
                    StandardCharsets.UTF_8);

            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            mustGit(work, "pull", "--rebase", "--autostash", "origin", "main");

            final TestCaseDto pulled = RealMapper.build().readValue(
                    Files.readString(work.resolve("Test Cases/login flow/" + testCases.getFirst().getId() + ".tc"),
                            StandardCharsets.UTF_8), TestCaseDto.class);

            assertEquals(pulled.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(review(), List.of(), "a clean pull leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void anEmptyRemoteNamesNoHeadBranch() {
        final String remoteInfo = mustGit(work, "remote", "show", "origin");

        assertTrue(remoteInfo.contains("HEAD branch:"), "git reports a HEAD branch line: " + remoteInfo);
        assertEquals(GitRefs.parseHeadBranch(remoteInfo), "", "an empty remote names no branch, so the push falls back to the local one");
    }

    @Test
    public void gitReportsNewTestCasesAsUntrackedWithQuotedPaths() {
        writeTestProject();

        final String status = mustGit(work, "status", "--porcelain", "-uall");

        assertTrue(status.contains("?? \"Test Cases/login flow/"),
                "a path with a space comes back quoted: " + status);
        assertEquals(GitRefs.parseStatus(status.lines().toList()).stream()
                .filter(entry -> entry.path().endsWith(".tc")).count(), 2);
    }
}
