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
                new TestDetailsPopup(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopup(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules)
        );
    }
}