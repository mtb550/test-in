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
import org.testin.editor.run.RunEditor;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.GenerateReportBtn;
import org.testin.editor.toolbar.components.GridViewBtn;
import org.testin.editor.toolbar.components.LightModeBtn;
import org.testin.editor.toolbar.components.ListViewBtn;
import org.testin.editor.toolbar.components.RefreshBtn;
import org.testin.editor.toolbar.components.ResultAnalysisBtn;
import org.testin.editor.toolbar.components.RunDetailsPopupBtn;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.editor.toolbar.components.StopExecutionBtn;
import org.testin.editor.toolbar.components.ToolbarItem;

import java.util.List;

public class RunToolbar extends AbstractToolbarPanel {
    private final @NotNull Project p;

    private final @NotNull RunEditor editor;

    public RunToolbar(final @NotNull Project p, final @NotNull RunEditor editor) {
        super(editor);
        this.p = p;
        this.editor = editor;
        layoutComponents();
    }

    // UC-EDITOR-PANEL-030, Rule-EDITOR-PANEL-129
    @Override
    public @NotNull List<ToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionBtn(editor, getCallbacks()::onStartExecutionClicked),
                new StopExecutionBtn(getCallbacks()::onStopExecutionClicked),
                new LightModeBtn(editor),
                new GenerateReportBtn(p, editor),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules, getCallbacks()::getAvailableGroups),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
        );
    }

    @Override
    protected @NotNull List<ToolbarItem> getTrailingComponents() {
        return List.of(new ResultAnalysisBtn(editor, getCallbacks()::onToolBarResultAnalysisClicked));
    }
}