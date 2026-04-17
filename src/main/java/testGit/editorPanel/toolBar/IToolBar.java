package testGit.editorPanel.toolBar;

public interface IToolBar {
    void onToolBarSearchValueChanged(final String query);

    void onToolBarFilterSelectedChanged();

    void onToolBarFilterResetted();

    void onToolBarDetailsSelectedChanged();

    void onToolBarRefreshClicked();
}
