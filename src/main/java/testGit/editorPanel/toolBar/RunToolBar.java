package testGit.editorPanel.toolBar;

import com.intellij.openapi.Disposable;
import testGit.editorPanel.toolBar.components.*;

import java.util.List;

public class RunToolBar extends AbstractToolbarPanel {
    public RunToolBar(final Disposable pDisposable, final IToolBar callbacks) {
        super(pDisposable, callbacks);
        layoutComponents();
    }

    @Override
    public List<IToolbarItem> getCustomComponents() {
        return List.of(
                new RefreshBtn(callbacks::onToolBarRefreshClicked),
                new DetailsPopup(callbacks::onToolBarDetailsSelectedChanged),
                new FilterPopup(callbacks::onToolBarFilterResetted, callbacks::onToolBarFilterSelectedChanged), new StartExecutionBtn(),
                new GenerateReportBtn()
        );
    }
}