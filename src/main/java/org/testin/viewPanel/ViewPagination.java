package org.testin.viewPanel;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.mappers.dto.TestCaseDto;

import java.util.ArrayList;
import java.util.List;

public class ViewPagination {
    private final @NotNull ViewPanel viewPanel;
    private @Nullable List<TestCaseDto> items;
    private int currentIndex = 0;
    @Getter
    private @Nullable ArrayList<String> currentPath;

    public ViewPagination(final @NotNull ViewPanel viewPanel) {
        this.viewPanel = viewPanel;
    }

    public void updateList(final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
        this.items = testCases;
        this.currentIndex = 0;
        this.currentPath = path;
    }

    public @Nullable TestCaseDto getCurrentItem() {
        if (items != null && !items.isEmpty() && currentIndex >= 0 && currentIndex < items.size()) {
            return items.get(currentIndex);
        }
        return null;
    }

    public void goNext() {
        if (hasNext()) {
            currentIndex++;
            ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
        }
    }

    public void goPrevious() {
        if (hasPrevious()) {
            currentIndex--;
            ApplicationManager.getApplication().invokeLater(viewPanel::refreshCurrentView);
        }
    }

    public boolean hasNext() {
        return items != null && currentIndex < items.size() - 1;
    }

    public boolean hasPrevious() {
        return items != null && currentIndex > 0;
    }

}