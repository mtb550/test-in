package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * One selectable row of a {@link TextFieldWithSelections}: an icon, the text
 * shown to the tester, an optional badge saying what kind of thing it is, an
 * optional muted hint explaining what the choice means, and the value the
 * dialog receives when this row is selected on submit.
 * <p>
 * Public since #29. A dialog with fixed choices still declares them through
 * {@code ComponentDialogBase.textFieldWithSelections().selection(...)} and never
 * names this type; a dialog that searches has to build its rows as the tester
 * types, which is outside the framework, so the row it builds is this.
 */
public record SelectionList<T>(@NotNull Icon icon, @NotNull String name, @NotNull String tag, @NotNull String hint, @NotNull T value) {

    /**
     * A row with no badge, which is every row a dialog with fixed choices
     * offers - the kind of thing is the whole list there, so saying it on each
     * row says nothing.
     */
    public static <T> @NotNull SelectionList<T> add(final @NotNull Icon icon, final @NotNull String name, final @NotNull String hint, final @NotNull T value) {
        return new SelectionList<>(icon, name, "", hint, value);
    }

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-072.
     * <p>
     * A row that says what kind of thing it is, for a list whose rows are not
     * all the same kind. Only the search answers with four kinds at once.
     */
    public static <T> @NotNull SelectionList<T> tagged(final @NotNull Icon icon, final @NotNull String name, final @NotNull String tag, final @NotNull String hint, final @NotNull T value) {
        return new SelectionList<>(icon, name, tag, hint, value);
    }
}
