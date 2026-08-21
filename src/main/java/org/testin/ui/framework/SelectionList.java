package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * One selectable row of a {@link TextFieldWithSelections}: an icon, the text
 * shown to the tester, an optional muted hint explaining what the choice
 * means, and the value the dialog receives when this row is selected on
 * submit. Framework-internal — dialogs declare rows through
 * {@code ComponentDialogBase.textFieldWithSelections().selection(...)}.
 */
record SelectionList<T>(@NotNull Icon icon, @NotNull String name, @NotNull String hint, @NotNull T value) {

    static <T> @NotNull SelectionList<T> add(final @NotNull Icon icon, final @NotNull String name,
                                             final @NotNull String hint, final @NotNull T value) {
        return new SelectionList<>(icon, name, hint, value);
    }
}
