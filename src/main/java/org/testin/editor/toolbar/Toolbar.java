package org.testin.editor.toolbar;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

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
     * The tester is writing what the run means. Default empty because only a run
     * has verdicts to analyse.
     */
    default void onToolBarResultAnalysisClicked() {
    }

    /**
     * Which project this editor is in, and which node it was opened on.
     * <p>
     * Asked rather than acted on, the way {@link #getAvailableModules()} is: the
     * editor knows what it is showing, and the toolbar decides what to do with
     * that. It is what lets one Details button serve both editors - the node
     * answers for itself, and a test set and a test run are both a
     * {@link DirectoryDto} carrying a marker.
     */
    @NotNull Project getProject();

    @NotNull DirectoryDto getEditedNode();

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
