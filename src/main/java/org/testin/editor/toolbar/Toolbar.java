package org.testin.editor.toolbar;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface Toolbar {
    /**
     * The query is deliberately not a parameter: both editors rebuild their
     * filtered list through {@code EditorFilters.of(toolBar)}, which reads the
     * text from the search field itself. Passing it as well gave the same value
     * two routes, and both implementations ignored the argument (#61).
     */
    void onToolBarSearchValueChanged();

    /**
     * ESC in the search field: focus returns to the editor's list, the filter
     * text stays. Abstract on purpose - every toolbar host has a search field,
     * so a silent no-op here would hide a dead ESC.
     */
    void onToolBarSearchFocusReleased();

    void onToolBarFilterSelectionChanged();

    void onToolBarFilterResetButtonClicked();

    void onToolBarDetailsSelectionChanged();

    void onToolBarRefreshButtonClicked();

    /**
     * The tester asked what this node is. Default empty because only the run
     * toolbar offers it today.
     */
    default void onToolBarNodeDetailsClicked() {
    }

    default void onToolBarSwitchedToListView() {
    }

    default void onToolBarSwitchedToGridView() {
    }

    default void onToolBarCreateTestCaseClicked() {
    }

    default void onStartExecutionClicked() {
    }

    default void onStopExecutionClicked() {
    }

    @NotNull Set<String> getAvailableModules();
}
