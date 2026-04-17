package testGit.editorPanel;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.toolBar.AbstractToolbarPanel;
import testGit.pojo.Group;
import testGit.pojo.Priority;
import testGit.pojo.dto.TestCaseDto;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

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
        final AbstractToolbarPanel baseToolBar = getToolBar();
        final String query = baseToolBar != null ? baseToolBar.getSearchTxtField().getQuery() : "";
        final Set<Group> groupFilter = baseToolBar != null ? baseToolBar.getSettings().getSelectedGroup() : Collections.emptySet();
        final Set<Priority> priorityFilter = baseToolBar != null ? baseToolBar.getSettings().getSelectedPriority() : Collections.emptySet();

        final List<TestCaseDto> allItems = getAllTestCaseDtos();
        if (allItems == null || allItems.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (allItems) {
            return allItems.stream()
                    .filter(tc -> {
                        final boolean matchesSearch = query.isEmpty() || tc.getDescription().toLowerCase().contains(query) || tc.getId().toString().toLowerCase().contains(query) || tc.getExpectedResult().toLowerCase().contains(query) || tc.getSteps().stream().anyMatch(step -> step != null && step.toLowerCase().contains(query));

                        final boolean matchesGroup = groupFilter.isEmpty() || tc.getGroup().stream().anyMatch(groupFilter::contains);

                        final boolean matchesPriority = priorityFilter.isEmpty() || priorityFilter.contains(tc.getPriority());

                        return matchesSearch && matchesGroup && matchesPriority;
                    })
                    .collect(Collectors.toList());
        }
    }

    default void dispose() {
        Optional.ofNullable(ViewToolWindowFactory.getViewPanel()).ifPresent(ViewPanel::reset);
    }
}