package testGit.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BaseEditorUI extends Disposable {
    StatusBar getStatusBar();

    int getCurrentPage();

    void setCurrentPage(int page);

    int getPageSize();

    void setPageSize(int size);

    int getTotalPageCount();

    int getTotalItemsCount();

    void refreshView();

    void appendNewTestCase(TestCaseDto tc);

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

    Set<String> getSelectedDetails();

    List<TestCaseDto> getAllTestCaseDtos();

    void updateSequenceAndSaveAll();

    void selectTestCase(TestCaseDto tc);

    Set<UUID> getUnsortedIds();

    String getHoveredIconAction();

    void setHoveredIconAction(String action);

    int getHoveredIndex();

    void setHoveredIndex(int index);

    default void dispose() {
        Optional.ofNullable(ViewToolWindowFactory.getViewPanel()).ifPresent(ViewPanel::reset);
    }
}