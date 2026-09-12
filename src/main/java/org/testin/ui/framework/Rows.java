package org.testin.ui.framework;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * What a {@link TextFieldWithSelections} should offer for what the tester has
 * typed so far (#29).
 * <p>
 * A dialog whose choices are fixed - the two create dialogs, which offer a node
 * kind - answers the same rows whatever the query is, and says so by declaring
 * them with {@code .selection(...)}. A dialog that searches answers different
 * rows for every query, and says so by declaring one of these.
 * <p>
 * Asked on a debounce and off nothing: it is handed the query and must answer
 * quickly, because it is asked while somebody is typing.
 */
@FunctionalInterface
public interface Rows<T> {

    @NotNull Answer<T> forQuery(final @NotNull String query);

    /**
     * UC-INTERNAL-001, Rule-INTERNAL-073.
     * <p>
     * What a query came back with: the rows to show, and one short thing to say
     * about them beside the field.
     * <p>
     * The note travels with the rows rather than being pushed in afterwards,
     * because the two have to change together - a count drawn beside a list it
     * does not describe is worse than no count. The words are the caller's: a
     * search says how many matched, and this component knows nothing about
     * searching.
     *
     * @param note said at the right of the field, empty when there is nothing to
     *             say - which is every dialog but the search
     */
    record Answer<T>(@NotNull List<SelectionList<T>> rows, @NotNull String note) {

        public static <T> @NotNull Answer<T> of(final @NotNull List<SelectionList<T>> rows) {
            return new Answer<>(rows, "");
        }
    }
}
