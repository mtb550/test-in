package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.UUID;

/**
 * Immutable page boundaries shared by the test and run editors.
 */
public record PageWindow(int page, int totalPages, int fromIndex, int toIndex) {

    public static @NotNull PageWindow of(final int itemCount, final int requestedPage, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        final int safeItemCount = Math.max(0, itemCount);
        final int totalPages = Math.max(1, (safeItemCount + safePageSize - 1) / safePageSize);
        final int page = Math.clamp(requestedPage, 1, totalPages);
        final int fromIndex = Math.min((page - 1) * safePageSize, safeItemCount);
        final int toIndex = Math.min(fromIndex + safePageSize, safeItemCount);
        return new PageWindow(page, totalPages, fromIndex, toIndex);
    }

    /**
     * The 1-based page holding this test case, or 0 when it is not in the list.
     * <p>
     * Both editors reload onto a page that may no longer hold the selected case,
     * and both worked it out with the same loop. The page size is guarded here
     * for the same reason {@link #of} guards it: it comes from settings, and
     * dividing by a stored 0 would throw.
     */
    public static int pageContaining(final @NotNull UUID testCaseId, final @NotNull List<TestCaseDto> items, final int pageSize) {
        final int safePageSize = Math.max(1, pageSize);
        for (int i = 0; i < items.size(); i++) {
            if (testCaseId.equals(items.get(i).getId())) return (i / safePageSize) + 1;
        }

        return 0;
    }

    public boolean isEmpty() {
        return fromIndex == toIndex;
    }
}
