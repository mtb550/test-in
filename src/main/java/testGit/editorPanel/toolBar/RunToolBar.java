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
                new StartExecutionBtn(getCallbacks()::onStartExecutionClicked),
                new GenerateReportBtn(),
                new RefreshBtn(getCallbacks()::onToolBarRefreshButtonClicked),
                new RunDetailsPopup(getCallbacks()::onToolBarDetailsSelectionChanged),
                new FilterPopup(getCallbacks()::onToolBarFilterResetButtonClicked, getCallbacks()::onToolBarFilterSelectionChanged)
        );
    }
}