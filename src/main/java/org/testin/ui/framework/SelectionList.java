package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * One selectable row of a {@link TextFieldWithSelections}: an icon, the text
 * shown to the tester, an optional muted hint explaining what the choice
 * means, and the value the dialog receives when this row is selected on
 * submit. Framework-internal — dialogs declare rows through
 * {@code ComponentDialogBase.textFieldWithSelections().selection(...)}.
 */
record SelectionList<T>(@Nullable Icon icon, @NotNull String name, @Nullable String hint, @NotNull T value) {

    static <T> @NotNull SelectionList<T> add(final @Nullable Icon icon, final @NotNull String name,
                                             final @Nullable String hint, final @NotNull T value) {
        return new SelectionList<>(icon, name, hint, value);
    }
}
