package org.testin.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.statusBar.StatusBar;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.DirectoryDto;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TestinEditor extends Disposable {
    @NotNull DirectoryDto getParent();

    @NotNull StatusBar getStatusBar();

    @NotNull AbstractToolbarPanel getToolBar();

    int getCurrentPage();

    void setCurrentPage(final int page);

    int getPageSize();

    void setPageSize(final int size);

    int getTotalPageCount();

    int getTotalItemsCount();

    void refreshView();

    @NotNull List<TestCaseDto> getSelectedTestCases();

    void appendNewTestCase(final @NotNull TestCaseDto tc);

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

    @NotNull Set<?> getSelectedDetails();

    @NotNull List<TestCaseDto> getAllTestCases();

    void updateSequenceAndSaveAll();

    void selectTestCase(final @Nullable TestCaseDto tc);

    @NotNull Set<UUID> getUnsortedIds();

    @Nullable String getHoveredIconAction();

    void setHoveredIconAction(final @Nullable String action);

    int getHoveredIndex();

    void setHoveredIndex(final int index);

    default void dispose() {
        Optional.ofNullable(ViewToolWindowFactory.getViewPanel()).ifPresent(ViewPanel::reset);
    }
}