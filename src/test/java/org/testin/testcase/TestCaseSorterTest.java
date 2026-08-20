package org.testin.testcase;

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

/**
 * The promise the test editor makes about a test set: every case in the folder
 * is on the screen, in an order that does not depend on the machine reading it.
 * <p>
 * That promise was broken twice in one evening, and neither break was in the
 * sorting - a copied file carrying another case's id took a case out of the
 * indexer, and the copy's empty pointers took the whole set's order with it.
 * Cases carry their own rank now, so neither is possible: there is no chain to
 * lose and nothing to be unreachable from.
 */
public class TestCaseSorterTest {

    private static TestCaseDto testCase(final String description, final String rank) {
        return TestCaseDto.builder()
                .id(UUID.randomUUID())
                .description(description)
                .order(rank)
                .createdAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    @Test
    public void casesAreShownInRankOrderWhateverOrderTheyWereRead() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        assertEquals(TestCaseSorter.sorted(List.of(third, first, second)), List.of(first, second, third));
        assertEquals(TestCaseSorter.sorted(List.of(second, third, first)), List.of(first, second, third));
    }

    /**
     * A case that arrived without a rank - copied in by hand, imported by an
     * older build, brought by a merge - is shown, at the end, where a tester
     * looks for something that just turned up. It is never dropped: what is in
     * the folder is on the screen.
     */
    @Test
    public void aCaseWithNoRankIsShownLastRatherThanHidden() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> sorted = TestCaseSorter.sorted(List.of(arrived, ranked));

        assertEquals(sorted.size(), 2);
        assertEquals(sorted, List.of(ranked, arrived));
    }

    /**
     * Two testers who each appended a case at the same moment produce the same
     * rank, and that is fine - what must never happen is two machines showing
     * two different orders.
     */
    @Test
    public void equalRanksStillGiveEveryMachineTheSameOrder() {
        final TestCaseDto mine = testCase("a signed-in user signs out", "s");
        final TestCaseDto theirs = testCase("a locked account cannot sign in", "s");

        assertEquals(TestCaseSorter.sorted(List.of(mine, theirs)), TestCaseSorter.sorted(List.of(theirs, mine)));
    }

    /**
     * What a drag means: the list the tester arranged becomes the order, and
     * only the case that actually moved is written.
     */
    @Test
    public void placingWritesOnlyTheCaseThatMoved() {
        final TestCaseDto first = testCase("sign in", "c");
        final TestCaseDto second = testCase("sign out", "m");
        final TestCaseDto third = testCase("a wrong password is refused", "s");

        final List<TestCaseDto> arranged = new ArrayList<>(List.of(first, third, second));
        final List<TestCaseDto> moved = TestCaseSorter.place(arranged);

        // One case is written, and which one is not knowable from the list: a
        // case dragged up and the case it passed swapping down arrange the same
        // way. What matters is that the set is not rewritten to say so.
        assertEquals(moved.size(), 1, "one drag, one file");
        assertEquals(TestCaseSorter.sorted(new ArrayList<>(arranged)), arranged, "and the list now sorts as arranged");
        assertEquals(first.getOrder(), "c", "the case at the top never moved, so its rank is untouched");
    }

    @Test
    public void placingGivesAnUnrankedCaseARank() {
        final TestCaseDto ranked = testCase("sign in", "c");
        final TestCaseDto arrived = testCase("copied in from somewhere", "");

        final List<TestCaseDto> moved = TestCaseSorter.place(new ArrayList<>(List.of(ranked, arrived)));

        assertEquals(moved, List.of(arrived));
        assertEquals(ranked.getOrder(), "c");
        assertEquals(TestCaseSorter.sorted(List.of(arrived, ranked)), List.of(ranked, arrived));
    }

    @Test
    public void anEmptySetSortsToNothingRatherThanFailing() {
        assertEquals(TestCaseSorter.sorted(List.of()), List.of());
        assertEquals(TestCaseSorter.place(new ArrayList<>()), List.of());
    }
}
