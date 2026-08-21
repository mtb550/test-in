package org.testin.testcase.update.bulk;

import org.jetbrains.annotations.NotNull;

/**
 * What a bulk edit did to one row: a value the tester typed, or nothing because
 * they never touched it.
 * <p>
 * Untouched is not the same as emptied, and the difference matters in both
 * directions. The editor shows a stored value with its newlines flattened to
 * spaces, so writing an untouched row back would flatten it permanently - while
 * a row the tester deliberately cleared is a change to apply, when the section
 * accepts a blank one.
 * <p>
 * That difference used to be carried by a null in a list of strings, which said
 * nothing about which of the two it meant and left every reader to remember.
 *
 * @param value   what to write, and empty on a row that was not edited
 * @param changed whether the tester edited this row at all
 */
public record EditedValue(@NotNull String value, boolean changed) {

    /**
     * A row the tester left alone. Nothing is written for it.
     */
    public static final @NotNull EditedValue UNCHANGED = new EditedValue("", false);

    public static @NotNull EditedValue of(final @NotNull String value) {
        return new EditedValue(value, true);
    }
}
