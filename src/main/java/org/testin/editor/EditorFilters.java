package org.testin.editor;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.model.Group;
import org.testin.model.Priority;

import java.util.Set;

/**
 * What the toolbar is currently narrowing the list by, read in one go.
 * <p>
 * Both editors opened their filtering with the same seven lines: the search
 * query, then the filter popup, then the three sets out of it.
 * <p>
 * Run status is deliberately not here. Only the run editor filters by it,
 * because only a run has one, and a field that half the callers must ignore is
 * worse than the caller that needs it asking for it.
 */
public record EditorFilters(@NotNull String query, @NotNull Set<Group> groups,
                            @NotNull Set<Priority> priorities, @NotNull Set<String> modules) {

    public static @NotNull EditorFilters of(final @NotNull AbstractToolbarPanel toolBar) {
        final FilterPopupBtn filters = toolBar.getToolbarItem(FilterPopupBtn.class);

        return new EditorFilters(
                toolBar.getSearchTxt().getSearchQuery(),
                filters.getSelectedGroup(),
                filters.getSelectedPriority(),
                filters.getSelectedModule());
    }
}
