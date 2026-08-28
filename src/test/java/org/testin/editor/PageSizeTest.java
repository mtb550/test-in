package org.testin.editor;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * What the page size field does with whatever a tester leaves in it.
 * <p>
 * Every answer is a usable page size, because the field is corrected in place
 * rather than refused: nothing here can report a problem, so nothing here may
 * have one.
 */
public class PageSizeTest {

    @Test
    public void aNumberInRangeIsTheNumber() {
        assertEquals(TestinEditor.pageSizeOf("25"), 25);
        assertEquals(TestinEditor.pageSizeOf("1"), 1, "one case per page is a small page, not a mistake");
    }

    /**
     * Surrounding space is a tester tabbing through a field, not a different
     * number.
     */
    @Test
    public void spaceAroundTheNumberIsIgnored() {
        assertEquals(TestinEditor.pageSizeOf("  25  "), 25);
    }

    @Test
    public void moreThanTheCeilingIsTheCeiling() {
        assertEquals(TestinEditor.pageSizeOf("5000"), TestinEditor.MAX_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(String.valueOf(TestinEditor.MAX_PAGE_SIZE)), TestinEditor.MAX_PAGE_SIZE);
    }

    /**
     * Zero, a negative and an empty field all say the same thing - that the
     * tester did not name a size - and one case per page is not what any of them
     * were reaching for.
     */
    @Test
    public void nothingUsableIsTheDefault() {
        assertEquals(TestinEditor.pageSizeOf("0"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("-3"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(""), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("   "), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("abc"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("2 5"), TestinEditor.DEFAULT_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf("25.5"), TestinEditor.DEFAULT_PAGE_SIZE);
    }

    /**
     * A key held down. It is still a tester asking for a very large page, so the
     * answer is the ceiling - parsing it and catching the overflow would have
     * answered fifty, which is the opposite of what they asked for.
     */
    @Test
    public void aNumberTooLongToHoldIsStillTooLarge() {
        assertEquals(TestinEditor.pageSizeOf("9".repeat(25)), TestinEditor.MAX_PAGE_SIZE);
        assertEquals(TestinEditor.pageSizeOf(String.valueOf(Long.MAX_VALUE)), TestinEditor.MAX_PAGE_SIZE);
    }
}
