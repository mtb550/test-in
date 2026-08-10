package org.testin.editorPanel;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PageWindowTest {

    @Test
    public void calculatesMiddlePageBounds() {
        final PageWindow page = PageWindow.of(125, 2, 50);

        assertEquals(page.page(), 2);
        assertEquals(page.totalPages(), 3);
        assertEquals(page.fromIndex(), 50);
        assertEquals(page.toIndex(), 100);
    }

    @Test
    public void clampsPageAndHandlesEmptyData() {
        final PageWindow page = PageWindow.of(0, 4, 50);

        assertEquals(page.page(), 1);
        assertEquals(page.totalPages(), 1);
        assertEquals(page.fromIndex(), 0);
        assertEquals(page.toIndex(), 0);
        assertTrue(page.isEmpty());
    }

    @Test
    public void protectsAgainstInvalidPageSize() {
        final PageWindow page = PageWindow.of(3, 2, 0);

        assertEquals(page.page(), 2);
        assertEquals(page.totalPages(), 3);
        assertEquals(page.fromIndex(), 1);
        assertEquals(page.toIndex(), 2);
    }
}
