package org.testin.util;

import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

/**
 * What a list has selected.
 * <p>
 * Swing says "nothing is selected" with a null, and five places asked about it
 * separately - a copy action, a run-item action, a shortcut menu and a field
 * that falls back to its first row. Converted here, so an empty selection reads
 * as the same thing everywhere it is read.
 * <p>
 * The tree's side of this is {@link org.testin.explorer.tree.TreeValueUtil}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListValue {

    public static <T> @NotNull Optional<T> selected(final @NotNull JList<T> list) {
        return Optional.ofNullable(list.getSelectedValue());
    }
}
