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

    @NotNull List<SelectionList<T>> forQuery(final @NotNull String query);
}
