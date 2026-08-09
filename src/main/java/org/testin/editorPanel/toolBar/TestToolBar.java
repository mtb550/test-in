package org.testin.editorPanel.toolBar;

import org.testin.editorPanel.toolBar.components.*;

import java.util.List;

public class TestToolBar extends AbstractToolbarPanel {

    public TestToolBar(final IToolBar callbacks) {
        super(callbacks);
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new CreateTestCaseBtn(getCallbacks()::onToolBarCreateTestCaseClicked),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new TestDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // todo, why tool bar search not here ?
        );
    }
}