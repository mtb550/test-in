package org.testin.git;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * What a pending change is about.
 * <p>
 * The review used to know one answer - a test case - and read every {@code .json}
 * in the repository as one. A test run parsed that way came out as a test case
 * with no description, which is why its row showed a blank name; worse, comparing
 * two revisions of it found no test-case field different, so an edited run
 * vanished from the review entirely, and since the commit stages only what the
 * review lists, it could never be committed from here (#66).
 * <p>
 * Every changed file now says which of these it is, so nothing is read as
 * something it is not and nothing drops out.
 */
@Getter
@AllArgsConstructor
public enum ChangeSubject {

    TEST_CASE(
            "Test Case"
    ),

    TEST_RUN(
            "Test Run"
    ),

    /**
     * The dotfile that makes a directory a node.
     * <p>
     * It carries no test data, so there is little in one to read. It is still
     * what a colleague pulling the commit needs to see the directory as a test
     * set at all. And a change to one - archiving a project, deprecating a test
     * set - is worth committing on its own.
     */
    MARKER(
            "Marker"
    ),

    /**
     * Anything else that turned up in the repository. Listed rather than
     * ignored: what the review does not show cannot be committed, and a file
     * nobody accounted for is exactly the one that goes missing.
     */
    OTHER(
            "File"
    );

    private final @NotNull String label;
}
