package org.testin.git;

import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.Mapper;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/**
 * The whole workflow against a real repository: write test cases, review what
 * changed, commit, push, and have a colleague clone what arrived.
 * <p>
 * Nothing is stubbed. Git is the Git on this machine, the repository is a real
 * one in a temporary directory, and the remote is a real bare repository - so a
 * push is a push. What is exercised is the plugin's own logic driving it: the
 * review comes from {@link GitDiffProcessor} reading real {@code git status}
 * output, and what gets staged is what {@link GitRefs} and
 * {@link GitCommitService} decide it should be.
 * <p>
 * The parsing tests elsewhere prove the rules are right about text we typed.
 * This proves they are right about text Git produced, which is the difference
 * between a test passing and the feature working - the review was empty for
 * every user of this plugin while its unit tests were green.
 */
public class GitWorkflowTest {

    private Path remote;
    private Path work;

    // ------------------------------------------------------------------ setup

    private static Mapper mapper() {
        try {
            final Constructor<Mapper> constructor = Mapper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (final ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not build a Mapper for the test", ex);
        }
    }

    /**
     * Runs Git and returns its output, or null when it failed - which is how the
     * plugin treats a failure too: no answer rather than an exception.
     */
    private static String git(final Path directory, final String... arguments) {
        final List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        try {
            final Process process = new ProcessBuilder(command)
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();

            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.waitFor() == 0 ? output : null;

        } catch (final IOException | InterruptedException ex) {
            return null;
        }
    }

    private static String mustGit(final Path directory, final String... arguments) {
        final String output = git(directory, arguments);
        assertNotNull(output, "git " + String.join(" ", arguments) + " failed in " + directory);
        return output;
    }

    @BeforeMethod
    public void createRepositories() {
        try {
            if (git(Path.of("."), "--version") == null) {
                throw new SkipException("Git is not on the PATH, so the workflow cannot be exercised");
            }

            final Path base = Files.createTempDirectory("testin-workflow");
            remote = base.resolve("remote.git");
            work = base.resolve("work");
            Files.createDirectories(remote);
            Files.createDirectories(work);

            mustGit(remote, "init", "--bare", "--initial-branch=main");
            mustGit(work, "init", "--initial-branch=main");

            // Local, so the test never depends on - or touches - the developer's own
            // Git identity.
            mustGit(work, "config", "user.name", "Testin Test");
            mustGit(work, "config", "user.email", "testin@example.invalid");
            mustGit(work, "remote", "add", "origin", remote.toUri().toString());
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositories() {
        try {
            if (remote == null) return;
            final Path base = remote.getParent();
            if (base == null || !Files.exists(base)) return;

            try (Stream<Path> paths = Files.walk(base)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    path.toFile().setWritable(true);
                    path.toFile().delete();
                });
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    // ------------------------------------------------------------ large commits

    /**
     * The Windows command-line limit, in characters. A process whose command
     * line is longer than this cannot be started at all, and the refusal names
     * neither the limit nor a path - it arrives as {@code CreateProcess
     * error=206}, which is what a tester saw instead of their commit.
     */
    private static final int WINDOWS_COMMAND_LINE_LIMIT = 32767;

    /**
     * A commit whose paths would not fit on a command line still lands.
     * <p>
     * The size is the test. An import brings in hundreds of test cases at once
     * and every one of them is a new file, so the first commit after an import
     * is the largest one a tester ever makes - and it was the one that could not
     * be made. 1,200 cases under a name holding a space is roughly what a
     * spreadsheet import produces, and about three times the limit.
     * <p>
     * Driven through {@link GitCommandRunner#pathspecBytes}, so what Git reads
     * here is what the plugin writes. Running the command is left to real Git
     * rather than to the runner, which needs a live IDE for git4idea.
     */
    @Test
    public void aCommitTooLargeForTheCommandLineStillLands() {
        try {
            mustGit(work, "commit", "--allow-empty", "-m", "root");

            final Path set = work.resolve("test-01").resolve("Test Cases").resolve("pkg1").resolve("Login");
            Files.createDirectories(set);

            final Set<String> paths = new LinkedHashSet<>();
            for (int i = 0; i < 1200; i++) {
                final String name = UUID.randomUUID() + ".json";
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

    // ------------------------------------------------------- the test project

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
            Files.writeString(file, content instanceof String text ? text : mapper().writeValueAsString(content),
                    StandardCharsets.UTF_8);

        } catch (final IOException ex) {

            throw new AssertionError(ex);

        }

    }

    /**
     * A test project as the plugin lays one out: a marker per directory, and the
     * test cases linked head to tail the way the editor orders them.
     */
    private List<TestCaseDto> writeTestProject() {
        write(work, ".tp", "{\"status\":\"ACTIVE\"}");
        write(work, "Test Cases/.tcd", "{}");
        write(work, "Test Runs/.trd", "{}");
        write(work, "Test Cases/login flow/.ts", "{}");

        final List<TestCaseDto> cases = List.of(
                testCase("a registered user signs in"),
                testCase("a wrong password is refused"));

        // Ranked in the order the editor would show them.
        TestCaseOrder.rankAll(cases);

        for (final TestCaseDto testCase : cases) {
            write(work, "Test Cases/login flow/" + testCase.getId() + ".json", testCase);
        }
        return cases;
    }

    // --------------------------------------------------------- the plugin bits

    /**
     * The review, built the way the plugin builds it: from what Git says changed
     * and what Git has committed.
     */
    private List<PendingChange> review() {
        final List<String> status = mustGit(work, "status", "--porcelain", "-uall")
                .lines().filter(line -> !line.isBlank()).toList();

        return GitDiffProcessor.toDiffs(status, work, mapper(),
                path -> git(work, "show", "HEAD:" + path));
    }

    /**
     * Everything the plugin would stage for the given review: the selected test
     * cases, and the markers that make their directories mean anything.
     */
    private Set<String> stagedFor(final List<PendingChange> review) {
        final Set<String> paths = new LinkedHashSet<>(GitRefs.repoRelativePaths(review));
        paths.addAll(GitCommitService.markersAlongside(work, paths));
        return paths;
    }

    private void commit(final Set<String> paths, final String message) {
        // Through the plugin's own rule, not a copy of it: which paths git add
        // may be given is the thing being tested when a rename is involved.
        final Set<String> stageable = GitCommitService.stageable(work, paths);

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

    // ------------------------------------------------------------------ tests

    /**
     * The first commit of a new test project, which is the case that could not be
     * made at all: everything is untracked, and untracked was invisible.
     */
    @Test
    public void aNewTestProjectIsReviewedCommittedAndPushed() {
        final List<TestCaseDto> cases = writeTestProject();

        final List<PendingChange> pending = review();
        assertEquals(pending.stream().filter(change -> change.subject() == ChangeSubject.TEST_CASE).count(), 2,
                "both new test cases are in the review");
        assertTrue(pending.stream().anyMatch(change -> change.subject() == ChangeSubject.MARKER),
                "and so are the markers that make their directories nodes");
        assertTrue(pending.stream().allMatch(diff -> diff.type() == DiffType.ADDED));

        commit(stagedFor(pending), "the first commit");
        assertNotNull(git(work, "push", "-u", "origin", "main"), "the push to an empty remote succeeded");

        final Path colleague = cloneAsColleague();
        for (final TestCaseDto testCase : cases) {
            assertTrue(Files.exists(colleague.resolve("Test Cases/login flow/" + testCase.getId() + ".json")),
                    "the colleague received " + testCase.getDescription());
        }
    }

    /**
     * The fault that would have made every clone useless: a directory is only a
     * test set because a marker sits in it, and the review never lists markers.
     */
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

    /**
     * A run directory is not part of a test case commit, so it must not be
     * dragged in: the markers that travel with a selection are the ones above
     * the cases in it.
     * <p>
     * Markers are rows of their own now, so a tester can commit one deliberately
     * - archiving a project is a marker edit and nothing else. This is about the
     * ones nobody selected: the review is filtered to the test cases, and what
     * comes along is only what those cases need to mean anything.
     */
    @Test
    public void onlyTheMarkersAboveTheSelectedCasesTravel() {
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

    /**
     * Editing a case and asking again: the review reads the committed side out of
     * Git and reports only the field that moved.
     */
    @Test
    public void editingACaseShowsExactlyWhatChanged() {
        final List<TestCaseDto> cases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        assertEquals(review(), List.of(), "nothing is pending straight after a commit");

        final TestCaseDto edited = cases.getFirst().setModule("payments");
        write(work, "Test Cases/login flow/" + edited.getId() + ".json", edited);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().type(), DiffType.MODIFIED);
        assertEquals(pending.getFirst().fieldChanges().size(), 1, "one field moved, so one row");
        assertEquals(pending.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_MODULE);
        assertEquals(pending.getFirst().fieldChanges().getFirst().oldValue(), "authentication");
        assertEquals(pending.getFirst().fieldChanges().getFirst().newValue(), "payments");
    }

    @Test
    public void addingACaseToACommittedTestSetIsReviewedAsAnAddition() {
        writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final TestCaseDto extra = testCase("a locked account cannot sign in");
        write(work, "Test Cases/login flow/" + extra.getId() + ".json", extra);

        final List<PendingChange> pending = review();

        assertEquals(pending.size(), 1);
        assertEquals(pending.getFirst().type(), DiffType.ADDED);
        assertEquals(pending.getFirst().testCase().getDescription(), "a locked account cannot sign in");
    }

    @Test
    public void deletingACaseIsReviewedFromWhatWasCommitted() {
        try {
            final List<TestCaseDto> cases = writeTestProject();
            commit(stagedFor(review()), "the first commit");

            Files.delete(work.resolve("Test Cases/login flow/" + cases.getFirst().getId() + ".json"));

            final List<PendingChange> pending = review();

            assertEquals(pending.size(), 1);
            assertEquals(pending.getFirst().type(), DiffType.DELETED);
            assertEquals(pending.getFirst().testCase().getDescription(), "a registered user signs in");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A rename the tester staged somewhere else - the IDE's own commit window,
     * or the command line - and then brought to this review.
     * <p>
     * Two things had to be true and neither was. The review has to list both
     * sides, or the commit carries the new file and leaves the old one behind,
     * and whoever pulls it has the test case twice. And the old path must be
     * kept out of {@code git add}, which refuses a path that is in neither the
     * working tree nor the index - one of those fails the command outright, so
     * the commit never happens.
     */
    @Test
    public void aRenameStagedElsewhereCommitsBothSides() {
        final List<TestCaseDto> cases = writeTestProject();
        commit(stagedFor(review()), "the first commit");

        final String file = cases.getFirst().getId() + ".json";
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

    /**
     * The conflict this product actually gets: a colleague edited one field of a
     * test case and the tester edited another, so Git stops on a file where
     * nobody disagreed about anything (#90).
     * <p>
     * Everything here is real - a remote, a colleague's clone, a rebase that
     * genuinely stops - because the merge rules being right is not the same as
     * the merge working. The three stages have to be readable while the rebase
     * is stopped, the merged file has to be one Git accepts as a resolution, and
     * the rebase has to finish afterward.
     */
    @Test
    public void aConflictedTestCaseIsMergedFieldByFieldAndTheRebaseFinishes() {
        try {
            final List<TestCaseDto> cases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final String relativePath = "Test Cases/login flow/" + cases.getFirst().getId() + ".json";

            // The colleague sharpens the expected result.
            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve(relativePath);
            final TestCaseDto theirs = mapper().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(theirCopy, mapper().writeValueAsString(
                    theirs.setExpectedResult("the dashboard opens within two seconds").setUpdatedBy("colleague")),
                    StandardCharsets.UTF_8);
            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            // The tester rewords the description of the same case, and commits.
            final Path myCopy = work.resolve(relativePath);
            final TestCaseDto mine = mapper().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(myCopy, mapper().writeValueAsString(
                    mine.setDescription("a registered user signs in with a valid password").setUpdatedBy("muteb")),
                    StandardCharsets.UTF_8);
            commit(stagedFor(review()), "reworded the description");

            // Git stops: one file, two commits, no way for it to know the two edits
            // are in different fields.
            assertNull(git(work, "pull", "--rebase", "--autostash", "origin", "main"),
                    "the pull is expected to stop on the conflict");

            final List<String> conflicting = GitRefs.unmergedPaths(
                    mustGit(work, "status", "--porcelain", "-uall").lines().filter(line -> !line.isBlank()).toList());
            assertEquals(conflicting, List.of(relativePath));

            // What the plugin does with it: read the three sides Git is holding and
            // merge them field by field.
            final String base = mustGit(work, "show", ":1:" + relativePath);
            final String remote = mustGit(work, "show", ":2:" + relativePath);
            final String replayed = mustGit(work, "show", ":3:" + relativePath);

            final TestCaseMerge.Merge merge = TestCaseMerge.of(mapper(), base, replayed, remote);
            assertTrue(merge.isSettled(), "different fields are not a disagreement");

            Files.writeString(myCopy, merge.merged().toPrettyString(), StandardCharsets.UTF_8);
            mustGit(work, "add", "--", relativePath);
            mustGit(work, "-c", "core.editor=true", "rebase", "--continue");

            // Both edits survived, and the repository is not mid-rebase any more.
            final TestCaseDto merged = mapper().readValue(Files.readString(myCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            assertEquals(merged.getDescription(), "a registered user signs in with a valid password");
            assertEquals(merged.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
            assertEquals(review(), List.of(), "a resolved rebase leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Two testers adding a test case to the same test set at the same time -
     * the thing this product does more than anything else, and the thing that
     * used to conflict (#90).
     * <p>
     * Neither new file conflicts: they are new files with new names. Nothing
     * else conflicts either, now that a case carries its own position - the case
     * that happened to be last used to be rewritten by both testers to point at
     * their own new one, and that third file was the conflict. Git merges this
     * on its own, with nothing for the plugin to resolve.
     */
    @Test
    public void twoTestersAddingCasesToOneSetDoNotConflictAtAll() {
        try {
            writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            // The colleague appends a case and pushes it.
            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final TestCaseDto theirNewCase = testCase("a locked account cannot sign in").setOrder("s");
            write(colleague, "Test Cases/login flow/" + theirNewCase.getId() + ".json", theirNewCase);

            mustGit(colleague, "add", "-A");
            mustGit(colleague, "commit", "-m", "added the locked account case");
            mustGit(colleague, "push", "origin", "main");

            // This tester appends one too, at the same moment.
            final TestCaseDto myNewCase = testCase("a signed-in user signs out").setOrder("s");
            write(work, "Test Cases/login flow/" + myNewCase.getId() + ".json", myNewCase);
            commit(stagedFor(review()), "added the sign out case");

            // No conflict to resolve: the pull rebases straight through.
            assertNotNull(git(work, "pull", "--rebase", "--autostash", "origin", "main"),
                    "two appended cases touch two files and merge on their own");

            final List<TestCaseDto> after = new ArrayList<>();
            try (Stream<Path> files = Files.list(work.resolve("Test Cases/login flow"))) {
                for (final Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    after.add(mapper().readValue(Files.readString(file, StandardCharsets.UTF_8), TestCaseDto.class));
                }
            }

            assertEquals(after.size(), 4, "both testers keep their case");

            // Same rank on both, which is allowed: the order is settled the same way
            // on every machine, so two testers never see two different lists.
            final List<TestCaseDto> ordered = TestCaseOrder.ordered(after);
            assertEquals(ordered, TestCaseOrder.ordered(new ArrayList<>(after.reversed())),
                    "the order does not depend on what order the files were read in");
            assertTrue(ordered.stream().anyMatch(tc -> tc.getId().equals(theirNewCase.getId())));
            assertTrue(ordered.stream().anyMatch(tc -> tc.getId().equals(myNewCase.getId())));
            assertEquals(mustGit(work, "status", "--porcelain", "-uall").strip(), "");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The colleague half of the round trip: they change a case and push, and the
     * change arrives here on a pull.
     */
    @Test
    public void aColleaguesChangeArrivesOnAPull() {
        try {
            final List<TestCaseDto> cases = writeTestProject();
            commit(stagedFor(review()), "the first commit");
            mustGit(work, "push", "-u", "origin", "main");

            final Path colleague = cloneAsColleague();
            mustGit(colleague, "config", "user.name", "Colleague");
            mustGit(colleague, "config", "user.email", "colleague@example.invalid");

            final Path theirCopy = colleague.resolve("Test Cases/login flow/" + cases.getFirst().getId() + ".json");
            final TestCaseDto theirs = mapper().readValue(Files.readString(theirCopy, StandardCharsets.UTF_8), TestCaseDto.class);
            Files.writeString(theirCopy, mapper().writeValueAsString(theirs.setExpectedResult("the dashboard opens within two seconds")),
                    StandardCharsets.UTF_8);

            mustGit(colleague, "commit", "-am", "tightened the expected result");
            mustGit(colleague, "push", "origin", "main");

            mustGit(work, "pull", "--rebase", "--autostash", "origin", "main");

            final TestCaseDto pulled = mapper().readValue(
                    Files.readString(work.resolve("Test Cases/login flow/" + cases.getFirst().getId() + ".json"),
                            StandardCharsets.UTF_8), TestCaseDto.class);

            assertEquals(pulled.getExpectedResult(), "the dashboard opens within two seconds");
            assertEquals(review(), List.of(), "a clean pull leaves nothing pending");
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A repository with no commits reports no HEAD branch, which was read as a
     * branch literally called "(unknown)" and broke every first push.
     */
    @Test
    public void anEmptyRemoteNamesNoHeadBranch() {
        final String remoteInfo = mustGit(work, "remote", "show", "origin");

        assertTrue(remoteInfo.contains("HEAD branch:"), "git reports a HEAD branch line: " + remoteInfo);
        assertEquals(GitRefs.parseHeadBranch(remoteInfo), "", "an empty remote names no branch, so the push falls back to the local one");
    }

    /**
     * The status output the review is built from, straight from Git rather than
     * typed into a test - quoting, untracked marks and all.
     */
    @Test
    public void gitReportsNewTestCasesAsUntrackedWithQuotedPaths() {
        writeTestProject();

        final String status = mustGit(work, "status", "--porcelain", "-uall");

        assertTrue(status.contains("?? \"Test Cases/login flow/"),
                "a path with a space comes back quoted: " + status);
        assertEquals(GitRefs.parseStatus(status.lines().toList()).stream()
                .filter(entry -> entry.path().endsWith(".json")).count(), 2);
    }
}
