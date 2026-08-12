package org.testin.editorPanel;

import org.jetbrains.annotations.NotNull;

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

    public boolean isEmpty() {
        return fromIndex == toIndex;
    }
}
