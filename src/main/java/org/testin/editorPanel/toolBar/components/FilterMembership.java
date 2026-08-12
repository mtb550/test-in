package org.testin.editorPanel.toolBar.components;

import java.util.Set;

/**
 * How toggling a filter value updates the selection set. Enums with their own
 * membership rules pass a method reference (e.g. {@code Priority::onChange});
 * plain values use {@link #plain()}.
 */
@FunctionalInterface
public interface FilterMembership<T> {

    static <T> FilterMembership<T> plain() {
        return (value, selection, selected) -> {
            if (selected) selection.add(value);
            else selection.remove(value);
        };
    }

    void apply(T value, Set<T> selection, boolean selected);
}
