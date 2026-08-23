package org.testin.model;

import org.jetbrains.annotations.NotNull;

/**
 * What a run grid cell does with the text the tester typed into it (#74).
 * <p>
 * The counterpart of {@link ValueExtractor}, which turns a run item into the
 * text a cell shows. A column that carries one of these can be typed into and a
 * column that carries {@link #NONE} cannot, so which columns the run grid lets a
 * tester edit is declared on the attribute rather than tested for at the table.
 */
@FunctionalInterface
public interface RunValueSetter {

    /**
     * Every column that is read: the row number, the description, the verdict,
     * who recorded it and when.
     * <p>
     * A value of its own rather than a null, so the attribute always has a
     * setter and {@link RunEditorAttributes#isEdited()} is the only place that
     * asks which kind it is.
     */
    @NotNull RunValueSetter NONE = (item, typed) -> {
    };

    void execute(final @NotNull TestRunItems item, final @NotNull String typed);
}
