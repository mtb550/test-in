package org.testin.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The four things on a run row that exist only to explain why a case is not
 * passing: what happened, the stacktrace behind it, and how bad the bug is.
 * <p>
 * They are declared here as one list because two places need the same answer
 * about them and used to hold their own copies of it:
 * {@link TestRunItems#recordVerdict} clears them when a case passes, and the
 * verdict has to say what it would erase before it does. A fifth such field
 * added to the row is one constant here, and both places already know about it.
 */
@Getter
@AllArgsConstructor
public enum FailureDetail {

    ACTUAL_RESULT(
            "the actual result",
            item -> !item.getActualResult().isBlank(),
            item -> item.setActualResult("")
    ),

    STACKTRACE(
            "the stacktrace",
            item -> !item.getStacktrace().isBlank(),
            item -> item.setStacktrace("")
    ),

    BUG_SEVERITY(
            "the bug severity",
            item -> item.getBugSeverity() != BugSeverity.EMPTY,
            item -> item.setBugSeverity(BugSeverity.EMPTY)
    ),

    BUG_PRIORITY(
            "the bug priority",
            item -> item.getBugPriority() != BugPriority.EMPTY,
            item -> item.setBugPriority(BugPriority.EMPTY)
    );

    /**
     * What it is called in the sentence that warns the tester it is about to go.
     * Lower case and with its article, because it is read inside a sentence
     * rather than as a heading.
     */
    private final @NotNull String label;

    private final @NotNull Predicate<TestRunItems> filled;

    private final @NotNull Consumer<TestRunItems> clear;

    /**
     * Everything this row holds that a pass would erase, in the tester's words,
     * and empty when a pass would erase nothing - which is the ordinary case.
     */
    public static @NotNull List<String> filledIn(final @NotNull TestRunItems item) {
        return Arrays.stream(values())
                .filter(detail -> detail.filled.test(item))
                .map(FailureDetail::getLabel)
                .toList();
    }

    /**
     * Puts every one of them back to its empty value.
     */
    public static void clearAll(final @NotNull TestRunItems item) {
        Arrays.stream(values()).forEach(detail -> detail.clear.accept(item));
    }
}
