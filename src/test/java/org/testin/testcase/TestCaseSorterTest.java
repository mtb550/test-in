package org.testin.testcase;

import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The promise the test editor makes about a test set: every case in the folder
 * is on the screen, and a case that is not linked into the order says so rather
 * than disappearing.
 * <p>
 * That promise was broken twice in one evening and neither break was in the
 * sorting. A copied file carrying another case's id took a case out of the
 * indexer entirely, and the copy's empty pointers took the head away from the
 * set - which is what the sorter reports by badging everything. The rules below
 * are what the editor rests on, so they are pinned here.
 */
public class TestCaseSorterTest {

    private static TestCaseDto testCase(final String description) {
        return TestCaseDto.builder().id(UUID.randomUUID()).description(description).build();
    }

    /**
     * Links the cases into a chain, first to last.
     */
    private static List<TestCaseDto> chained(final TestCaseDto... cases) {
        for (int i = 0; i < cases.length; i++) {
            cases[i].setIsHead(i == 0);
            cases[i].setNext(i < cases.length - 1 ? cases[i + 1].getId() : null);
        }
        return List.of(cases);
    }

    @Test
    public void aLinkedSetIsInChainOrderAndNothingIsUnsorted() {
        final TestCaseDto first = testCase("sign in");
        final TestCaseDto second = testCase("sign out");
        final TestCaseDto third = testCase("a wrong password is refused");

        final SortResult result = TestCaseSorter.sortTestCases(chained(first, second, third));

        assertEquals(result.sortedList(), List.of(first, second, third));
        assertEquals(result.unsortedIds(), Set.of());
    }

    /**
     * The case this exists for: a file arrives that nothing points at - copied
     * in, pulled in, merged in - and it belongs on the screen with a badge, not
     * in a log line.
     */
    @Test
    public void aCaseNothingPointsAtIsShownAndBadged() {
        final TestCaseDto first = testCase("sign in");
        final TestCaseDto second = testCase("sign out");
        final TestCaseDto orphan = testCase("arrived from somewhere else");

        chained(first, second);

        final SortResult result = TestCaseSorter.sortTestCases(List.of(first, second, orphan));

        assertEquals(result.sortedList().size(), 3, "every case in the folder is on the screen");
        assertEquals(result.sortedList().getLast(), orphan, "and the one nothing points at is last");
        assertEquals(result.unsortedIds(), Set.of(orphan.getId()));
    }

    /**
     * No head at all - which is what a copied file with empty pointers leaves
     * behind. Nothing is dropped; the whole set is badged, because the order
     * genuinely is not known.
     */
    @Test
    public void aSetWithNoHeadIsAllShownAndAllBadged() {
        final TestCaseDto first = testCase("sign in");
        final TestCaseDto second = testCase("sign out");

        final SortResult result = TestCaseSorter.sortTestCases(List.of(first, second));

        assertEquals(result.sortedList().size(), 2);
        assertEquals(result.unsortedIds(), Set.of(first.getId(), second.getId()));
    }

    /**
     * A chain that points back at itself ends rather than running forever, and
     * the cases outside the loop are still shown.
     */
    @Test
    public void aCycleEndsAndEverythingIsStillShown() {
        final TestCaseDto first = testCase("sign in");
        final TestCaseDto second = testCase("sign out");
        final TestCaseDto third = testCase("a wrong password is refused");

        chained(first, second, third);
        third.setNext(first.getId());

        final SortResult result = TestCaseSorter.sortTestCases(List.of(first, second, third));

        assertEquals(result.sortedList().size(), 3);
        assertTrue(result.sortedList().containsAll(List.of(first, second, third)));
    }

    /**
     * A pointer at a case that is not in the folder - the other half of a case
     * someone deleted by hand - ends the chain, and what is left is still shown.
     */
    @Test
    public void aPointerAtAMissingCaseDoesNotHideTheRest() {
        final TestCaseDto first = testCase("sign in");
        final TestCaseDto second = testCase("sign out");

        first.setIsHead(true);
        first.setNext(UUID.randomUUID());
        second.setIsHead(false);
        second.setNext(null);

        final SortResult result = TestCaseSorter.sortTestCases(List.of(first, second));

        assertEquals(result.sortedList().size(), 2, "the case the chain never reached is still on the screen");
        assertEquals(result.unsortedIds(), Set.of(second.getId()));
    }

    @Test
    public void anEmptySetSortsToNothingRatherThanFailing() {
        final SortResult result = TestCaseSorter.sortTestCases(List.of());

        assertEquals(result.sortedList(), List.of());
        assertEquals(result.unsortedIds(), Set.of());
    }
}
