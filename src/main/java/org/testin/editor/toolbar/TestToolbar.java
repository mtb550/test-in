package org.testin.editor.toolbar;

import org.jetbrains.annotations.NotNull;
import org.testin.editor.toolbar.components.CreateTestCaseBtn;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.GridViewBtn;
import org.testin.editor.toolbar.components.ListViewBtn;
import org.testin.editor.toolbar.components.RefreshBtn;
import org.testin.editor.toolbar.components.TestDetailsPopupBtn;
import org.testin.editor.toolbar.components.ToolbarItem;

import java.util.List;

public class TestToolbar extends AbstractToolbarPanel {

    public TestToolbar(final @NotNull Toolbar callbacks) {
        super(callbacks);
        layoutComponents();
    }

    // UC-EDITOR-PANEL-001
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