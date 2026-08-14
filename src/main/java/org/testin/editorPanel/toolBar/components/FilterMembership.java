package org.testin.editorPanel.toolBar.components;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * How toggling a filter value updates the selection set.
 * <p>
 * Every filter uses {@link #plain()} today. The seam is here for a value whose
 * membership is not one-in-one-out — picking a parent that selects its children,
 * or a value that excludes another. Priority, Group and BugPriority each carried
 * their own copy of the plain rule until #61 found all three were identical to
 * this one.
 */
@FunctionalInterface
public interface FilterMembership<T> {

    static <T> @NotNull FilterMembership<T> plain() {
        return (value, selection, selected) -> {
            if (selected) selection.add(value);
            else selection.remove(value);
        };
    }

    void apply(final @NotNull T value, final @NotNull Set<T> selection, final boolean selected);
}
