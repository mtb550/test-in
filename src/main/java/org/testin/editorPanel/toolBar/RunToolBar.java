package org.testin.editorPanel.toolBar;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.editorPanel.toolBar.components.*;

import java.util.List;

public class RunToolBar extends AbstractToolbarPanel {
    private final @NotNull Project p;

    public RunToolBar(final @NotNull Project p, final IToolBar callbacks) {
        super(callbacks);
        this.p = p;
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionBtn(getCallbacks(), getCallbacks()::onStartExecutionClicked),
                new GenerateReportBtn(p),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // todo, why tool bar search not here ?
        );
    }
}