package org.testin.editor.toolBar;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolBar.components.*;

import java.util.List;

public class TestToolBar extends AbstractToolbarPanel {

    public TestToolBar(final @NotNull ToolBar callbacks) {
        super(callbacks);
        layoutComponents();
    }

    @Override
    public @NotNull List<ToolbarItem> getCustomComponents() {
        return List.of(
                new CreateTestCaseBtn(getCallbacks()::onToolBarCreateTestCaseClicked),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new TestDetailsPopupBtn(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopupBtn(getCallbacks(), getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged, getCallbacks()::getAvailableModules),
                new ListViewBtn(getCallbacks()::onToolBarSwitchedToListView),
                new GridViewBtn(getCallbacks()::onToolBarSwitchedToGridView)
                // The search field is created and laid out by AbstractToolbarPanel itself
                // because it needs its own horizontal-fill constraints.
        );
    }
}