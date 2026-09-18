/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.editor.toolbar;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;

import java.util.Set;

public interface Toolbar {

    /**
     * Whether the cases on this toolbar's editor carry a run status worth
     * filtering on.
     * <p>
     * A test case has no status; one executed in a run does. Declared here for
     * the reason the no-op events are: what a run does and a test editor does
     * not is this interface's to say, and the alternative was every button
     * testing what class it had been handed.
     */
    default boolean hasRunStatuses() {
        return false;
    }

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
     * has verdicts to analyze.
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

    /**
     * Every group the project has used, for the filter menu to offer.
     * <p>
     * The project's, not this editor's - unlike the modules above, which are
     * built from the test set on screen. A group is a word a tester types and
     * the cache holds every one of them, so filtering by a group used in another
     * test set is a question worth being able to ask (#296).
     */
    @NotNull Set<String> getAvailableGroups();
}
