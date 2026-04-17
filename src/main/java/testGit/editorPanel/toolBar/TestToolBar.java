package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.*;

import java.util.List;

public class TestToolBar extends AbstractToolbarPanel {
    private FilterPopup filter;

    public TestToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable, callbacks);
        layoutComponents();
    }

    @Override
    protected void updateFilterPopupState() {
        if (filter != null) {
            filter.updateState();
        }
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        // todo: move all filter login inside filter class.
        this.filter = new FilterPopup(settings, this::resetFilters, callbacks::onToolBarFilterSelectedChanged);

        return List.of(
                new RefreshBtn(callbacks::onToolBarRefreshClicked),
                new DetailsPopup(settings, callbacks::onToolBarDetailsSelectedChanged),
                filter,
                new CreateTestCaseBtn()
        );
    }
}