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

    /**
     * The editor this toolbar belongs to, typed as what it is.
     * <p>
     * Three of the buttons below are on this toolbar and no other, and each
     * asked at runtime whether the callbacks it was handed happened to be a run
     * editor - a question that could not be false. Declaring it here lets the
     * compiler answer instead, and the three guards go.
     */
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
                // Laid out beside Start and never removed: RunEditor flips which of the
                // two is visible, the way the list and grid view buttons already swap.
                new StopExecutionBtn(getCallbacks()::onStopExecutionClicked),
                new LightModeBtn(editor),
                new GenerateReportBtn(p, editor),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // The search field is created and laid out by AbstractToolbarPanel itself
                // because it needs its own horizontal-fill constraints.
        );
    }

    @Override
    protected @NotNull List<ToolbarItem> getTrailingComponents() {
        return List.of(new ResultAnalysisBtn(editor, getCallbacks()::onToolBarResultAnalysisClicked));
    }
}