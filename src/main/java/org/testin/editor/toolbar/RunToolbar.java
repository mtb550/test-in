package org.testin.editor.toolbar;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.*;

import java.util.List;

public class RunToolbar extends AbstractToolbarPanel {
    private final @NotNull Project p;

    public RunToolbar(final @NotNull Project p, final @NotNull Toolbar callbacks) {
        super(callbacks);
        this.p = p;
        layoutComponents();
    }

    @Override
    public @NotNull List<ToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionBtn(getCallbacks(), getCallbacks()::onStartExecutionClicked),
                new GenerateReportBtn(p),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // The search field is created and laid out by AbstractToolbarPanel itself
                // because it needs its own horizontal-fill constraints.
        );
    }
}