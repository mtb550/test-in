package org.testin.testcase;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The one promise ranks make: between any two of them there is another, so
 * moving a test case writes that case and nothing else.
 * <p>
 * Everything the ordering rests on is here. A rank that sorted wrong would put
 * a tester's cases in an order they did not choose, and a rank that could not be
 * squeezed between two others would send the plugin back to rewriting a whole
 * test set to move one row.
 */
public class RankTest {

    @Test
    public void theFirstCaseInAnEmptySetGetsRoomOnBothSides() {
        final String first = Rank.between("", "");

        assertTrue(first.compareTo(Rank.between("", first)) > 0, "something fits before it");
        assertTrue(first.compareTo(Rank.after(first)) < 0, "something fits after it");
    }

    @Test
    public void appendingSortsAfterWhatIsThere() {
        String last = Rank.between("", "");

        for (int i = 0; i < 200; i++) {
            final String next = Rank.after(last);
            assertTrue(last.compareTo(next) < 0, "append " + i + ": " + last + " then " + next);
            last = next;
        }
    }

    @Test
    public void aRankAlwaysFitsBetweenTwoOthers() {
        String low = Rank.between("", "");
        String high = Rank.after(low);

        // Dropping a case into the same gap over and over is the worst case a
        // tester can produce, and it has to keep working.
        for (int i = 0; i < 200; i++) {
            final String middle = Rank.between(low, high);

            assertTrue(low.compareTo(middle) < 0, "drop " + i + ": " + low + " < " + middle);
            assertTrue(middle.compareTo(high) < 0, "drop " + i + ": " + middle + " < " + high);

            high = middle;
        }
    }

    @Test
    public void aRankFitsBeforeTheFirstCase() {
        String first = Rank.between("", "");

        for (int i = 0; i < 100; i++) {
            final String earlier = Rank.between("", first);

            assertTrue(earlier.compareTo(first) < 0, "insert at top " + i + ": " + earlier + " < " + first);
            first = earlier;
        }
    }

    @Test
    public void aSpreadIsInOrderAndLeavesGaps() {
        final List<String> ranks = Rank.spread(10);

        assertEquals(ranks.size(), 10);

        for (int i = 1; i < ranks.size(); i++) {
            assertTrue(ranks.get(i - 1).compareTo(ranks.get(i)) < 0,
                    "spread is ordered: " + ranks.get(i - 1) + " < " + ranks.get(i));

            final String between = Rank.between(ranks.get(i - 1), ranks.get(i));
            assertTrue(ranks.get(i - 1).compareTo(between) < 0 && between.compareTo(ranks.get(i)) < 0,
                    "and something fits in every gap");
        }
    }

    /**
     * A set bigger than the alphabet still gets distinct ranks in order - a
     * project with three hundred cases in one set is a real test suite, not an
     * edge case.
     */
    @Test
    public void aLargeSpreadStaysDistinctAndOrdered() {
        final List<String> ranks = Rank.spread(300);
        final List<String> sorted = new ArrayList<>(ranks);
        sorted.sort(String::compareTo);

        assertEquals(ranks, sorted, "a spread is already in sort order");
        assertEquals(ranks.stream().distinct().count(), 300L, "and holds no duplicates");
    }

    /**
     * Past the alphabet, appending grows the rank by a character rather than
     * running out: z is followed by zm, which sorts after it because a longer
     * string with the same prefix does. Not za - a is the zero digit, so za
     * would be the same position as z written twice, with no room between them.
     */
    @Test
    public void appendingPastTheAlphabetGrowsTheRank() {
        assertEquals(Rank.after("y"), "z");
        assertEquals(Rank.after("z"), "zm");
        assertEquals(Rank.after("zz"), "zzm");

        assertTrue("z".compareTo(Rank.after("z")) < 0, "and the longer rank still sorts after");
    }

    /**
     * A set of any size is written with as many characters as it needs and no
     * more. A thousand cases used to produce a rank seventy-one characters long,
     * because the spread stepped one letter at a time and then piled zs on the
     * end once the alphabet ran out.
     */
    @Test
    public void aSpreadStaysShortHoweverManyCasesThereAre() {
        assertEquals(Rank.spread(20).stream().mapToInt(String::length).max().orElse(0), 1);
        assertEquals(Rank.spread(500).stream().mapToInt(String::length).max().orElse(0), 2);
        assertEquals(Rank.spread(1000).stream().mapToInt(String::length).max().orElse(0), 3);
    }

    @Test
    public void anEmptySpreadIsEmptyRatherThanAFailure() {
        assertEquals(Rank.spread(0), List.of());
    }
}
