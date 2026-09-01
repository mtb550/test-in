package org.testin.testrun;

import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;

/**
 * What the next cycle is called (#9).
 * <p>
 * The whole of the rule is here and none of it is visible where the answer is
 * used, which is the reason it has a class and a test rather than three lines in
 * an action.
 */
public class NextRunNameTest {

    @Test
    public void aTrailingNumberIsIncremented() {
        assertEquals(NextRunName.after("cycle-1", Set.of()), "cycle-2");
        assertEquals(NextRunName.after("cycle 9", Set.of()), "cycle 10");
        assertEquals(NextRunName.after("2", Set.of()), "3");
    }

    /**
     * The number is as long as it can be, so a padded one counts as one number
     * rather than a stem ending in a digit.
     */
    @Test
    public void aPaddedNumberIsStillOneNumber() {
        assertEquals(NextRunName.after("run09", Set.of()), "run10");
    }

    @Test
    public void aNameWithNoNumberGetsOne() {
        // Separated, because "smoke2" reads as a different word and "smoke-2"
        // reads as the second of them.
        assertEquals(NextRunName.after("smoke", Set.of()), "smoke-2");
    }

    /**
     * The obvious next name is routinely already there - re-creating cycle-1 in
     * a folder that already holds cycle-2 and cycle-3.
     */
    @Test
    public void aTakenNameIsCountedPast() {
        assertEquals(NextRunName.after("cycle-1", Set.of("cycle-2", "cycle-3")), "cycle-4");
        assertEquals(NextRunName.after("smoke", Set.of("smoke-2")), "smoke-3");
    }

    /**
     * A number longer than any cycle count still increments, because the rule
     * reads at most the last nine digits - so the parse cannot overflow and the
     * name still counts up. Pinned because the alternative reading, that too
     * long is not a number at all, is the one that looks right in the pattern.
     */
    @Test
    public void aNumberTooLongToParseWholeStillCountsUp() {
        assertEquals(NextRunName.after("build-12345678901234567890", Set.of()), "build-12345678901234567891");
    }
}
