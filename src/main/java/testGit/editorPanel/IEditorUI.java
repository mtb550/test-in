package testGit.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.toolBar.AbstractToolbarPanel;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IEditorUI extends Disposable {
    StatusBar getStatusBar();

    AbstractToolbarPanel getToolBar();

    int getCurrentPage();

    void setCurrentPage(final int page);

    int getPageSize();

    void setPageSize(final int size);

    int getTotalPageCount();

    int getTotalItemsCount();

    void refreshView();

    List<TestCaseDto> getSelectedTestCases();

    void appendNewTestCase(final TestCaseDto tc);

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

    Set<?> getSelectedDetails();

    List<TestCaseDto> getAllTestCaseDtos();

    List<TestCaseDto> getCurrentTestCaseDtos();

    void updateSequenceAndSaveAll();

    void selectTestCase(final TestCaseDto tc);

    Set<UUID> getUnsortedIds();

    String getHoveredIconAction();

    void setHoveredIconAction(final String action);

    int getHoveredIndex();

    void setHoveredIndex(final int index);

    default void dispose() {
        Optional.ofNullable(ViewToolWindowFactory.getViewPanel()).ifPresent(ViewPanel::reset);
    }
}