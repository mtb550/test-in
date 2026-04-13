package testGit.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.toolBar.ToolBar;
import testGit.pojo.Groups;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public interface BaseEditorUI extends Disposable {
    StatusBar getStatusBar();

    ToolBar getToolBar();

    int getCurrentPage();

    void setCurrentPage(final int page);

    int getPageSize();

    void setPageSize(final int size);

    int getTotalPageCount();

    int getTotalItemsCount();

    void refreshView();

    void appendNewTestCase(final TestCaseDto tc);

    @NotNull JComponent getComponent();

    @Nullable JComponent getPreferredFocusedComponent();

    Set<String> getSelectedDetails();

    List<TestCaseDto> getAllTestCaseDtos();

    void updateSequenceAndSaveAll();

    void selectTestCase(final TestCaseDto tc);

    Set<UUID> getUnsortedIds();

    String getHoveredIconAction();

    void setHoveredIconAction(final String action);

    int getHoveredIndex();

    void setHoveredIndex(final int index);

    default List<TestCaseDto> getFilteredList() {
        final ToolBar toolBar = getToolBar();
        final String query = toolBar != null ? toolBar.getSearchQuery() : "";
        final Set<Groups> groups = toolBar != null ? toolBar.getSettings().getSelectedGroups() : Collections.emptySet();
        final Set<Priority> priorityFilters = toolBar != null ? toolBar.getSettings().getSelectedPriorities() : Collections.emptySet();

        final List<TestCaseDto> allItems = getAllTestCaseDtos();
        if (allItems == null || allItems.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (allItems) {
            return allItems.stream()
                    .filter(tc -> {
                        final boolean matchesSearch = query.isEmpty() || tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query) || tc.getId().toString().toLowerCase().contains(query) || tc.getExpected() != null && tc.getExpected().toLowerCase().contains(query) || tc.getSteps() != null && tc.getSteps().stream().anyMatch(step -> step != null && step.toLowerCase().contains(query));

                        final boolean matchesGroup = groups.isEmpty() || (tc.getGroups() != null && tc.getGroups().stream().anyMatch(groups::contains));

                        final boolean matchesPriority = priorityFilters.isEmpty() || tc.getPriority() != null && priorityFilters.contains(tc.getPriority());

                        return matchesSearch && matchesGroup && matchesPriority;
                    })
                    .collect(Collectors.toList());
        }
    }

    default void dispose() {
        Optional.ofNullable(ViewToolWindowFactory.getViewPanel()).ifPresent(ViewPanel::reset);
    }
}