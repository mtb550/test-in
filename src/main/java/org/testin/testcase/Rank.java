package org.testin.testcase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The position of a test case in its test set, as a value the case owns.
 * <p>
 * Order used to be a chain: each case named the next one, and one case was the
 * head. That made a tester's own edit rewrite a case belonging to someone else -
 * appending wrote the previous last case, inserting wrote its neighbor, deleting
 * wrote the one before it - so two testers working in parallel conflicted on a
 * file neither of them meant to touch, and a single lost pointer left a whole
 * test set with no order at all.
 * <p>
 * A rank is a string, and cases are shown in the order those strings sort in.
 * Between any two ranks there is always room for another, so a case can be put
 * anywhere by writing that one case: nothing else in the set moves, and the
 * commit says exactly what the tester did.
 * <p>
 * The alphabet is {@code a}-{@code z}, read as digits after a decimal point -
 * {@code b} is before {@code c}, and {@code bm} is between them. A rank never
 * ends in {@code a}, because {@code a} is the zero digit: {@code ma} and
 * {@code m} would be the same position written two ways, and nothing could ever
 * be squeezed between them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Rank {

    private static final char FIRST = 'a';
    private static final char LAST = 'z';

    /**
     * Below every digit, for reading a rank that has run out of characters: a
     * rank that ended is a rank with zeroes after it.
     */
    private static final char BELOW_FIRST = FIRST - 1;

    /**
     * Above every digit, for the same reason on the other side: nothing at all
     * on the right is the largest thing there is.
     */
    private static final char ABOVE_LAST = LAST + 1;

    /**
     * The rank of the first case in an empty set. Mid-alphabet on purpose:
     * whatever arrives next, before it or after it, gets a rank without either
     * end of the alphabet being reached.
     */
    public static final @NotNull String MIDDLE = "m";

    /**
     * A rank between two others, either of which may be empty for "nothing on
     * that side".
     * <p>
     * This is the whole point of ranks: dropping a case between two others is
     * one string and one file, whatever else is in the set.
     */
    public static @NotNull String between(final @NotNull String before, final @NotNull String after) {
        if (after.isEmpty()) return after(before);

        return midpoint(before, after);
    }

    /**
     * A rank that sorts after everything: the case being appended to a set.
     */
    public static @NotNull String after(final @NotNull String last) {
        if (last.isEmpty()) return MIDDLE;

        final char lastChar = last.charAt(last.length() - 1);

        // Room left in this position: step one letter up and stay the same
        // length, which keeps ranks short for the ordinary case of appending.
        if (lastChar < LAST) {
            return last.substring(0, last.length() - 1) + (char) (lastChar + 1);
        }

        // Ends in z, so nothing of this length sorts after it: one character
        // longer, in the middle, leaving room on both sides of that too.
        return last + MIDDLE;
    }

    /**
     * Ranks for a whole list at once, evenly spread - a set being written from
     * nothing by an import, or by the one-time conversion of a set that still
     * carried a chain.
     * <p>
     * Spread rather than consecutive so that later insertions stay short:
     * {@code c, f, i} leaves whole letters free between them, where {@code a, b,
     * c} would force a second character at the first drop.
     */
    public static @NotNull List<String> spread(final int count) {
        if (count <= 0) return List.of();

        final List<String> ranks = new ArrayList<>(count);
        String last = "";

        final int room = LAST - FIRST;
        final int step = Math.max(1, room / (count + 1));

        for (int i = 0; i < count; i++) {
            last = last.isEmpty() ? String.valueOf((char) (FIRST + step)) : step(last, step);
            ranks.add(last);
        }

        return List.copyOf(ranks);
    }

    /**
     * The next rank a whole step further on, falling back to the smallest step
     * there is once the alphabet runs out - which keeps a set of any size in
     * order, however many cases it holds.
     */
    private static @NotNull String step(final @NotNull String from, final int step) {
        final char lastChar = from.charAt(from.length() - 1);

        if (lastChar + step <= LAST) {
            return from.substring(0, from.length() - 1) + (char) (lastChar + step);
        }

        return after(from);
    }

    /**
     * A rank strictly between two, digit by digit.
     * <p>
     * Where the two differ by more than one letter, the letter in the middle
     * ends it. Where they are equal or adjacent so far, the rank keeps that
     * digit and looks at the next one - which is why two ranks are never so
     * close together that nothing fits between them.
     * <p>
     * An empty {@code before} means nothing on that side, read as zeroes, so
     * this is also how a case is put above everything else.
     */
    private static @NotNull String midpoint(final @NotNull String before, final @NotNull String after) {
        final StringBuilder rank = new StringBuilder();

        for (int i = 0; ; i++) {
            final char low = i < before.length() ? before.charAt(i) : BELOW_FIRST;
            final char high = i < after.length() ? after.charAt(i) : ABOVE_LAST;

            if (high - low > 1) {
                return settled(rank.append((char) ((low + high) / 2)));
            }

            rank.append(low == BELOW_FIRST ? FIRST : low);
        }
    }

    /**
     * The zero digit is never the last one. A rank ending in {@code a} is the
     * same position as the rank without it, so nothing could be put between the
     * two - one more digit in the middle settles it, and stays below whatever
     * this rank was built to sit under.
     */
    private static @NotNull String settled(final @NotNull StringBuilder rank) {
        if (rank.charAt(rank.length() - 1) == FIRST) rank.append(MIDDLE);

        return rank.toString();
    }
}
