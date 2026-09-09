package org.testin.util;

import org.testin.model.TestCaseStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * The Status grid cell reads back what it wrote.
 * <p>
 * It printed getDisplayText() and parsed with TestCaseStatus.valueOf, so
 * retyping the word on screen threw an IllegalArgumentException nothing caught
 * and the tester got an IDE internal-error report instead of an edit. "To Be
 * Updated" was never a legal answer; "Disabled" was, only because that constant
 * happens to be named in mixed case rather than in capitals.
 */
public class TestCaseStatusParseTest {

    @Test
    public void everyStatusCanBeTypedBackExactlyAsItIsShown() {
        for (final TestCaseStatus status : TestCaseStatus.values()) {
            assertEquals(TestDataParser.testCaseStatus(status.getLabel(), TestCaseStatus.PENDING).orElseThrow(), status,
                    status.getLabel() + " is what the cell prints, so it has to be what the cell accepts");
        }
    }

    @Test
    public void theConstantNameIsAcceptedToo() {
        assertEquals(TestDataParser.testCaseStatus("TO_BE_UPDATED", TestCaseStatus.PENDING).orElseThrow(), TestCaseStatus.TO_BE_UPDATED);
    }

    @Test
    public void caseAndSurroundingSpaceDoNotMatter() {
        assertEquals(TestDataParser.testCaseStatus("  reviewed  ", TestCaseStatus.PENDING).orElseThrow(), TestCaseStatus.REVIEWED);
    }

    /**
     * Not a default, and not a throw: the value the case already had. The cell
     * redraws with it, so a typo reads as "that did not take" rather than as a
     * crash or as a status nobody chose.
     */
    @Test
    public void anythingElseKeepsTheStatusTheCaseAlreadyHad() {
        assertTrue(TestDataParser.testCaseStatus("Nonsense", TestCaseStatus.REVIEWED).isEmpty(), "a word Testin cannot read is refused, so the caller can say so");
        assertEquals(TestDataParser.testCaseStatus("", TestCaseStatus.DISABLED).orElseThrow(), TestCaseStatus.DISABLED);
        assertEquals(TestDataParser.testCaseStatus("   ", TestCaseStatus.TO_BE_UPDATED).orElseThrow(), TestCaseStatus.TO_BE_UPDATED);
    }
}
