package testGit.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.pojo.dto.TestCaseDto;

import javax.swing.*;
import java.util.List;
import java.util.Set;

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

    void onFilterChanged();

    void onDetailsChanged();

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

    boolean isShowGroups();

    boolean isShowPriority();

    Set<String> getSelectedDetails();

    List<TestCaseDto> getAllTestCaseDtos();

    void updateSequenceAndSaveAll();

    void selectTestCase(TestCaseDto tc);

    Set<String> getUnsortedIds();
}