package org.testin.git;

import org.testin.model.Priority;
import org.testin.model.dto.TestCaseDto;
import org.testin.util.Mapper;
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The step between "Git says these files changed" and "here is the review".
 * <p>
 * Everything here is real except Git itself: real files in a real directory, and
 * the status lines Git actually prints. What is stubbed is only the committed
 * side of each change, because reading that is the one thing that needs a
 * repository with history.
 * <p>
 * This is where the two faults that made the feature unusable lived - a new test
 * case never appearing, and the whole review coming back empty - so it is worth
 * asserting against the output Git really produces rather than a tidied version
 * of it.
 */
public class GitDiffProcessorTest {

    private final Map<String, String> committed = new HashMap<>();
    private Path root;

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
    public void createRepositoryRoot() {
        try {
            root = Files.createTempDirectory("testin-diff");
            committed.clear();
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @AfterMethod
    public void removeRepositoryRoot() {
        try {
            if (root == null || !Files.exists(root)) return;
            try (Stream<Path> paths = Files.walk(root)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private TestCaseDto testCase(final String description) {
        return TestCaseDto.builder()
                .description(description)
                .expectedResult("the balance is shown")
                .steps(new ArrayList<>(List.of("open the app", "log in")))
                .priority(Priority.LOW)
                .module("payments")
                .build();
    }

    /**
     * Writes a file into the working tree exactly as the plugin would.
     */
    private void onDisk(final String relativePath, final TestCaseDto testCase) {
        try {
            final Path file = root.resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, mapper().writeValueAsString(testCase), StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private List<PendingChange> review(final String... statusLines) {
        return GitDiffProcessor.toDiffs(List.of(statusLines), root, mapper(), committed::get);
    }

    /**
     * The fault that made the feature unusable: a test case just written is
     * untracked, and it has to appear.
     */
    @Test
    public void aNewlyWrittenTestCaseIsInTheReview() {
        onDisk("Test Cases/login/case.json", testCase("a brand new case"));

        final List<PendingChange> review = review("?? \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.ADDED);
        assertEquals(review.getFirst().testCase().getDescription(), "a brand new case");
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CREATE_TEST_CASE);
    }

    @Test
    public void anEditedTestCaseListsOnlyTheFieldsThatMoved() {
        final TestCaseDto before = testCase("the original");
        final TestCaseDto after = testCase("the original").setId(before.getId()).setModule("billing");

        committed.put("Test Cases/login/case.json", mapper().writeValueAsString(before));
        onDisk("Test Cases/login/case.json", after);

        final List<PendingChange> review = review(" M \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.MODIFIED);
        assertEquals(review.getFirst().fieldChanges().size(), 1);
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_MODULE);
    }

    /**
     * A deleted file is not on disk, so its content can only come from the
     * committed side.
     */
    @Test
    public void aDeletedTestCaseIsReviewedFromWhatWasCommitted() {
        final TestCaseDto removed = testCase("a case that is going away");
        committed.put("Test Cases/login/case.json", mapper().writeValueAsString(removed));

        final List<PendingChange> review = review(" D \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.DELETED);
        assertEquals(review.getFirst().testCase().getDescription(), "a case that is going away");
    }

    /**
     * Markers travel with a test case commit, and they are also changes in their
     * own right: archiving a project edits nothing but a marker, and a review
     * that hid them left the tester unable to commit it (#66).
     */
    @Test
    public void markersAreListedAsMarkerChanges() {
        try {
            onDisk("Test Cases/login/case.json", testCase("the test case among them"));
            Files.writeString(root.resolve("Test Cases/login/.ts"), "{}", StandardCharsets.UTF_8);
            Files.writeString(root.resolve(".tp"), "{}", StandardCharsets.UTF_8);

            final List<PendingChange> review = review(
                    "?? .tp",
                    "?? \"Test Cases/login/.ts\"",
                    "?? \"Test Cases/login/case.json\"");

            assertEquals(review.size(), 3, "every changed file is a row - what is not listed cannot be committed");
            assertEquals(review.stream().filter(change -> change.subject() == ChangeSubject.MARKER).count(), 2);
            assertEquals(review.stream().filter(change -> change.subject() == ChangeSubject.TEST_CASE).count(), 1);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Git reports a file as modified for reasons no compared field shows - a
     * reorder, an audit stamp. It is still a change, and the commit stages only
     * what the review lists, so it gets a row rather than disappearing (#66).
     */
    @Test
    public void aFileGitCallsModifiedIsAlwaysARow() {
        final TestCaseDto unchanged = testCase("identical on both sides");

        committed.put("Test Cases/login/case.json", mapper().writeValueAsString(unchanged));
        onDisk("Test Cases/login/case.json", unchanged);

        final List<PendingChange> review = review(" M \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CHANGE_FILE);
    }

    /**
     * The whole of a new test set arrives as untracked files, one line each -
     * which is what {@code -uall} is for. Every one of them is a row.
     */
    @Test
    public void awholeNewTestSetIsReviewedCaseByCase() {
        try {
            for (int index = 1; index <= 3; index++) {
                onDisk("Test Cases/login flow/case-" + index + ".json", testCase("case " + index));
            }
            Files.writeString(root.resolve("Test Cases/login flow/.ts"), "{}", StandardCharsets.UTF_8);

            final List<PendingChange> review = review(
                    "?? \"Test Cases/login flow/.ts\"",
                    "?? \"Test Cases/login flow/case-1.json\"",
                    "?? \"Test Cases/login flow/case-2.json\"",
                    "?? \"Test Cases/login flow/case-3.json\"");

            assertEquals(review.size(), 4, "three cases and the marker that makes the directory a test set");
            assertTrue(review.stream().allMatch(diff -> diff.type() == DiffType.ADDED));
            assertEquals(review.stream().filter(diff -> diff.subject() == ChangeSubject.TEST_CASE).count(), 3);
        } catch (final IOException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * A path with a space is every Testin repository - the two fixed containers
     * are called "Test Cases" and "Test Runs" - so the quoted form has to survive
     * all the way to reading the file off disk.
     */
    @Test
    public void aQuotedPathStillFindsItsFile() {
        onDisk("Test Cases/login flow/a case.json", testCase("quoted all the way down"));

        final List<PendingChange> review = review("?? \"Test Cases/login flow/a case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().relativeFilePath(), Path.of("Test Cases/login flow/a case.json"));
        assertEquals(review.getFirst().testCase().getDescription(), "quoted all the way down");
    }

    /**
     * Git named a new file that is no longer there - it listed the status a
     * moment before something removed the file. There is nothing to commit and
     * nothing to show, and one such file must not take the rest of the review
     * with it: the tester still has to be able to commit everything else (#66).
     */
    @Test
    public void anUntrackedFileThatVanishedIsSkippedAndTheRestSurvives() {
        onDisk("Test Cases/login/case.json", testCase("still here"));

        final List<PendingChange> review = review(
                "?? \"Test Cases/login/gone.json\"",
                "?? \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1, "the file that vanished is not a change; the one that is there still is");
        assertEquals(review.getFirst().testCase().getDescription(), "still here");
    }

    @Test
    public void nothingChangedIsAnEmptyReview() {
        assertEquals(review(), List.of());
    }
}
