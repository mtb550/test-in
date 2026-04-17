package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.*;

import java.util.List;

public class TestToolBar extends AbstractToolbarPanel {

    public TestToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable, callbacks);
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new CreateTestCaseBtn(callbacks::onToolBarCreateTestCaseClicked),
                new RefreshBtn(callbacks::onToolBarRefreshButtonClicked),
                new DetailsPopup(callbacks::onToolBarDetailsSelectionChanged),
                new FilterPopup(callbacks::onToolBarFilterResetButtonClicked, callbacks::onToolBarFilterSelectionChanged)
        );
    }
}