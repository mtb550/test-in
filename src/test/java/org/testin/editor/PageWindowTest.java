package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PageWindowTest {

    private static @NotNull List<TestCaseDto> casesOf(final int count) {
        final List<TestCaseDto> cases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final TestCaseDto tc = new TestCaseDto();
            tc.setId(UUID.randomUUID());
            cases.add(tc);
        }
        return cases;
    }

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

    @Test
    public void findsThePageHoldingATestCase() {
        final List<TestCaseDto> cases = casesOf(125);

        assertEquals(PageWindow.pageContaining(cases.get(0).getId(), cases, 50), 1);
        assertEquals(PageWindow.pageContaining(cases.get(49).getId(), cases, 50), 1);
        assertEquals(PageWindow.pageContaining(cases.get(50).getId(), cases, 50), 2);
        assertEquals(PageWindow.pageContaining(cases.get(124).getId(), cases, 50), 3);
    }

    /**
     * Zero, not one: the callers tell "it is on the first page" from "it is not
     * here any more" by this, and restoring a selection that has gone would put
     * the editor on a page the tester did not ask for.
     * <p>
     * Nothing remembered at all is no longer a case here - the editors hold the
     * remembered id as an Optional and do not ask when it is empty (#71).
     */
    @Test
    public void answersZeroWhenTheCaseIsNotInTheList() {
        assertEquals(PageWindow.pageContaining(UUID.randomUUID(), casesOf(10), 50), 0);
        assertEquals(PageWindow.pageContaining(UUID.randomUUID(), List.of(), 50), 0);
    }

    @Test
    public void protectsAgainstInvalidPageSizeWhenSearching() {
        // A stored page size of 0 would divide by zero rather than return a page.
        final List<TestCaseDto> cases = casesOf(3);

        assertEquals(PageWindow.pageContaining(cases.get(2).getId(), cases, 0), 3);
    }
}
