package org.testin.editorPanel.toolBar;

import org.testin.editorPanel.toolBar.components.*;

import java.util.List;

public class RunToolBar extends AbstractToolbarPanel {
    public RunToolBar(final IToolBar callbacks) {
        super(callbacks);
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new StartExecutionBtn(getCallbacks(), getCallbacks()::onStartExecutionClicked),
                new GenerateReportBtn(),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopup(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopup(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules)
        );
    }
}