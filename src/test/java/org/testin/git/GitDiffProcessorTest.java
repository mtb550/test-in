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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.expectThrows;
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

    private Path root;
    private final Map<String, String> committed = new HashMap<>();

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
    public void createRepositoryRoot() throws IOException {
        root = Files.createTempDirectory("testin-diff");
        committed.clear();
    }

    @AfterMethod
    public void removeRepositoryRoot() throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
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
    private void onDisk(final String relativePath, final TestCaseDto testCase) throws IOException {
        final Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, mapper().writeValueAsString(testCase), StandardCharsets.UTF_8);
    }

    private List<TestCaseDiff> review(final String... statusLines) {
        return GitDiffProcessor.toDiffs(List.of(statusLines), root, mapper(), committed::get);
    }

    /**
     * The fault that made the feature unusable: a test case just written is
     * untracked, and it has to appear.
     */
    @Test
    public void aNewlyWrittenTestCaseIsInTheReview() throws IOException {
        onDisk("Test Cases/login/case.json", testCase("a brand new case"));

        final List<TestCaseDiff> review = review("?? \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.ADDED);
        assertEquals(review.getFirst().subject().getDescription(), "a brand new case");
        assertEquals(review.getFirst().fieldChanges().getFirst().changeType(), ChangeType.CREATE_TEST_CASE);
    }

    @Test
    public void anEditedTestCaseListsOnlyTheFieldsThatMoved() throws IOException {
        final TestCaseDto before = testCase("the original");
        final TestCaseDto after = testCase("the original").setId(before.getId()).setModule("billing");

        committed.put("Test Cases/login/case.json", mapper().writeValueAsString(before));
        onDisk("Test Cases/login/case.json", after);

        final List<TestCaseDiff> review = review(" M \"Test Cases/login/case.json\"");

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

        final List<TestCaseDiff> review = review(" D \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().type(), DiffType.DELETED);
        assertEquals(review.getFirst().subject().getDescription(), "a case that is going away");
    }

    /**
     * Markers change on every status edit and travel with the commit, but there
     * is nothing in one for a tester to read - so they must not become rows.
     */
    @Test
    public void markersAreNotReviewableRows() throws IOException {
        onDisk("Test Cases/login/case.json", testCase("the only reviewable thing here"));
        Files.writeString(root.resolve("Test Cases/login/.ts"), "{}", StandardCharsets.UTF_8);
        Files.writeString(root.resolve(".tp"), "{}", StandardCharsets.UTF_8);

        final List<TestCaseDiff> review = review(
                "?? .tp",
                "?? \"Test Cases/login/.ts\"",
                "?? \"Test Cases/login/case.json\"");

        assertEquals(review.size(), 1, "only the test case is reviewable");
    }

    /**
     * Git reports a file as modified for reasons a tester has no interest in.
     * A row with nothing in it is worse than no row.
     */
    @Test
    public void aFileGitCallsModifiedButIsNotShowsNoRow() throws IOException {
        final TestCaseDto unchanged = testCase("identical on both sides");

        committed.put("Test Cases/login/case.json", mapper().writeValueAsString(unchanged));
        onDisk("Test Cases/login/case.json", unchanged);

        assertEquals(review(" M \"Test Cases/login/case.json\""), List.of());
    }

    /**
     * The whole of a new test set arrives as untracked files, one line each -
     * which is what {@code -uall} is for. Every one of them is a row.
     */
    @Test
    public void awholeNewTestSetIsReviewedCaseByCase() throws IOException {
        for (int index = 1; index <= 3; index++) {
            onDisk("Test Cases/login flow/case-" + index + ".json", testCase("case " + index));
        }

        final List<TestCaseDiff> review = review(
                "?? \"Test Cases/login flow/.ts\"",
                "?? \"Test Cases/login flow/case-1.json\"",
                "?? \"Test Cases/login flow/case-2.json\"",
                "?? \"Test Cases/login flow/case-3.json\"");

        assertEquals(review.size(), 3);
        assertTrue(review.stream().allMatch(diff -> diff.type() == DiffType.ADDED));
    }

    /**
     * A path with a space is every Testin repository - the two fixed containers
     * are called "Test Cases" and "Test Runs" - so the quoted form has to survive
     * all the way to reading the file off disk.
     */
    @Test
    public void aQuotedPathStillFindsItsFile() throws IOException {
        onDisk("Test Cases/login flow/a case.json", testCase("quoted all the way down"));

        final List<TestCaseDiff> review = review("?? \"Test Cases/login flow/a case.json\"");

        assertEquals(review.size(), 1);
        assertEquals(review.getFirst().relativeFilePath(), Path.of("Test Cases/login flow/a case.json"));
        assertEquals(review.getFirst().subject().getDescription(), "quoted all the way down");
    }

    /**
     * Git named a file that is not there. Reading the review must fail saying
     * which one, not carry on and show a diff against nothing.
     */
    @Test
    public void aChangedFileThatIsMissingFailsByName() {
        final IllegalStateException failure = expectThrows(IllegalStateException.class,
                () -> review("?? \"Test Cases/login/gone.json\""));

        assertNotNull(failure.getMessage());
        assertTrue(failure.getMessage().contains("gone.json"), "the message names the file: " + failure.getMessage());
    }

    @Test
    public void nothingChangedIsAnEmptyReview() {
        assertEquals(review(), List.of());
    }
}
