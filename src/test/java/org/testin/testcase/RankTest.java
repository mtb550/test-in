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

    @Test
    public void anEmptySpreadIsEmptyRatherThanAFailure() {
        assertEquals(Rank.spread(0), List.of());
    }
}
