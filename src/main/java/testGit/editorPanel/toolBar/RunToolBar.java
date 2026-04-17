package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.*;

import java.util.List;

public class RunToolBar extends AbstractToolbarPanel {

    private FilterPopup filterPopup;

    public RunToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable, callbacks);
        layoutComponents();
    }

    @Override
    protected void updateFilterPopupState() {
        if (filterPopup != null) {
            filterPopup.updateState();
        }
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        // todo: move all filter login inside filter class.
        this.filterPopup = new FilterPopup(settings, this::resetFilters, callbacks::onToolBarFilterSelectedChanged);

        return List.of(
                new RefreshBtn(callbacks::onToolBarRefreshClicked),
                new DetailsPopup(settings, callbacks::onToolBarDetailsSelectedChanged),
                filterPopup,
                new ExecuteTestCaseBtn(),
                new GenerateReportBtn()
        );
    }
}