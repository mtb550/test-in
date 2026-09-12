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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.util.Bundle;

import javax.swing.*;
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
     * What starting a manual execution is called, wherever it is offered.
     * <p>
     * "Manual" is in the name because the gesture runs no automation: it walks
     * the run a case at a time and times the tester reading each one. The
     * toolbar button called it "Start Execution" and the context menu entry
     * called it "Start Run", which read as the thing beside it in that menu -
     * Run Test Case, which does run code - and a tester who picked the wrong
     * one watched the first case get selected and a clock start while nothing
     * ran.
     */
    @NotNull String START_MANUAL_EXECUTION = Bundle.message("toolbar.start.manual.execution");

    /**
     * And the icon, for the same reason. The menu entry drew
     * {@code AllIcons.Actions.Execute}, which is the platform's run arrow and
     * the same family as the one on Run Test Case - so the two entries agreed
     * in the one way that mattered and disagreed in every other.
     */
    @NotNull Icon START_MANUAL_EXECUTION_ICON = AllIcons.Toolwindows.ToolWindowRun;

    /**
     * What stops it, and the icon that draws it.
     * <p>
     * Beside the two above for the reason they are there: two surfaces show
     * these controls now - the run editor's toolbar and light mode's title bar
     * (#13) - and a tester looking at one and then the other has to see the
     * same pair of buttons. Light mode drew the platform's run arrow, which is
     * the very icon the comment above records as the wrong one.
     */
    @NotNull String STOP_EXECUTION = Bundle.message("toolbar.stop.execution");

    @NotNull Icon STOP_EXECUTION_ICON = AllIcons.Debugger.ThreadFrozen;
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
