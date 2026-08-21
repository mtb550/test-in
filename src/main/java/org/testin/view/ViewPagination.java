package org.testin.view;

import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.model.dto.TestCaseDto;

import java.util.List;
import java.util.Optional;

/**
 * Which of the shown test cases the view panel is on, and how to step through
 * them. Showing nothing is an empty list rather than no list, so every question
 * here has an answer without asking whether there is one (#71).
 */
public class ViewPagination {
    private final @NotNull ViewPanel viewPanel;
    private @NotNull List<TestCaseDto> items = List.of();
    private int currentIndex = 0;

    /**
     * The tree path of what is being shown, for the navigation bar. Empty when
     * the panel was handed no path - the gutter and the grid both have one, and
     * a plain selection may not.
     */
    @Getter
    private @NotNull List<String> currentPath = List.of();

    public ViewPagination(final @NotNull ViewPanel viewPanel) {
        this.viewPanel = viewPanel;
    }

    public void updateList(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        this.items = testCases;
        this.currentIndex = 0;
        this.currentPath = path;
    }

    /**
     * The case on display, empty while the panel is showing none.
     */
    public @NotNull Optional<TestCaseDto> getCurrentItem() {
        return currentIndex >= 0 && currentIndex < items.size()
                ? Optional.of(items.get(currentIndex))
                : Optional.empty();
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
        return currentIndex < items.size() - 1;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

}
