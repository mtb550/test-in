package org.testin.testrun;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What to call the next cycle, given the one it follows.
 * <p>
 * A test cycle is a test run, and testers number them - cycle-1, cycle-2. So a
 * run re-created from cycle-1 is offered cycle-2, and one re-created from a name
 * with no number on it is offered that name with one. Either way the tester can
 * type over it; this only saves them the arithmetic.
 * <p>
 * Its own class because it is the whole of a rule with three cases and an edge,
 * and none of them is visible from the call site that uses the answer.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NextRunName {

    /**
     * The trailing number, and whatever comes before it. Reluctant on the left
     * so the number is as long as it can be: "run09" is run and 09, not run0
     * and 9.
     */
    private static final @NotNull Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d{1,9})$");

    /**
     * The first name after this one that nothing has taken.
     * <p>
     * Counted up rather than tried once, because the obvious next name is
     * routinely already there: re-creating cycle-1 in a folder that already
     * holds cycle-2 offers cycle-3, not a name the create would refuse.
     */
    public static @NotNull String after(final @NotNull String sourceName, final @NotNull Set<String> taken) {
        final @NotNull Matcher trailing = TRAILING_NUMBER.matcher(sourceName);
        final boolean numbered = trailing.matches();

        // A name with no number gets one, separated - "smoke" becomes "smoke-2"
        // rather than "smoke2", which reads as a different word.
        final @NotNull String stem = numbered ? trailing.group(1) : sourceName + "-";
        final int first = numbered ? Integer.parseInt(trailing.group(2)) + 1 : 2;

        int next = first;
        while (taken.contains(stem + next)) next++;

        return stem + next;
    }
}
