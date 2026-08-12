package org.testin.editorPanel.toolBar.components;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * How toggling a filter value updates the selection set. Enums with their own
 * membership rules pass a method reference (e.g. {@code Priority::onChange});
 * plain values use {@link #plain()}.
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
