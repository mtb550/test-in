package testGit.editorPanel.toolBar;

public interface IToolBar {
    void onToolBarSearchValueChanged(final String query);

    void onToolBarFilterSelectionChanged();

    void onToolBarFilterResetButtonClicked();

    void onToolBarDetailsSelectionChanged();

    void onToolBarRefreshButtonClicked();

    default void onToolBarCreateTestCaseClicked() {
    }

    default void onStartExecutionClicked() {
    }
}
