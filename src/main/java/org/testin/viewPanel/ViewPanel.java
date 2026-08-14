package org.testin.viewPanel;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.ViewTab;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.util.FontSync;
import org.testin.viewPanel.details.DetailsTab;
import org.testin.viewPanel.history.HistoryTab;
import org.testin.viewPanel.openBugs.OpenBugsTab;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ViewPanel implements Disposable {
    private final @NotNull JBPanel<?> detailsTab;
    private final @NotNull JBPanel<?> historyTab;
    private final @NotNull JBPanel<?> openBugsTab;

    @Getter
    private final @NotNull JBScrollPane detailsScrollPane;
    @Getter
    private final @NotNull JBScrollPane historyScrollPane;
    @Getter
    private final @NotNull JBScrollPane openBugsScrollPane;

    @Getter
    private final @NotNull ViewPagination page;

    @Getter
    private final @NotNull Project p;

    public ViewPanel(final @NotNull Project p) {
        this.p = p;
        Disposer.register(p, this);
        detailsTab = new JBPanel<>(new BorderLayout());
        historyTab = new JBPanel<>(new BorderLayout());
        openBugsTab = new JBPanel<>(new BorderLayout());

        FontSync.syncWithNativeEditor(p, detailsTab, this);
        FontSync.syncWithNativeEditor(p, historyTab, this);
        FontSync.syncWithNativeEditor(p, openBugsTab, this);

        detailsScrollPane = createScrollPane(detailsTab);
        historyScrollPane = createScrollPane(historyTab);
        openBugsScrollPane = createScrollPane(openBugsTab);

        page = new ViewPagination(this);

        refreshCurrentView();

        new ViewPanelExecutionSubscriber(p, this);
    }

    private @NotNull JBScrollPane createScrollPane(final @NotNull Component view) {
        final JBScrollPane sp = new JBScrollPane(view);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public void show(final @NotNull Project p, final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
        final ToolWindow tw = ViewToolWindowFactory.getToolWindow(p);
        if (tw == null || testCases == null || testCases.isEmpty()) return;

        tw.show(() -> {
            this.selectContent(ViewTab.DETAILS);
            this.updateList(testCases, path);
        });
    }

    public void show(final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
        this.show(p, testCases, path);
    }

    public @NotNull ViewPanel hide() {
        final ToolWindow tw = ViewToolWindowFactory.getToolWindow(p);
        if (tw != null && tw.isVisible()) {
            tw.hide(null);
        }
        return this;
    }

    public void reset() {
        this.updateList(null, null);
    }

    private void selectContent(final @NotNull ViewTab tab) {
        final ToolWindow tw = ViewToolWindowFactory.getToolWindow(p);
        if (tw == null) return;

        final Content[] contents = tw.getContentManager().getContents();
        for (final Content content : contents) {
            if (tab.getDisplayName().equals(content.getDisplayName())) {
                tw.getContentManager().setSelectedContent(content);
                break;
            }
        }
    }

    public void hide(final @Nullable TestCaseDto testCaseDtoToMatch) {
        final ToolWindow tw = ViewToolWindowFactory.getToolWindow(p);
        if (tw == null || !tw.isVisible()) return;

        final TestCaseDto currentlyShown = this.getCurrentTestCaseDto();

        if (currentlyShown != null && testCaseDtoToMatch != null &&
                currentlyShown.getId().equals(testCaseDtoToMatch.getId())) {
            this.reset();
            this.hide();
        }
    }

    public void updateList(final @Nullable List<TestCaseDto> testCases, final @Nullable ArrayList<String> path) {
        this.page.updateList(testCases, path);
        this.refreshCurrentView();
    }

    public void refreshCurrentView() {
        final TestCaseDto currentTestCaseDto = this.getCurrentTestCaseDto();
        final ArrayList<String> currentPath = this.page.getCurrentPath();

        new DetailsTab().load(p, detailsTab, currentTestCaseDto, currentPath);
        new HistoryTab().load(historyTab);
        new OpenBugsTab().load(openBugsTab);

        detailsTab.revalidate();
        detailsTab.repaint();
    }

    /**
     * Refreshes the panel when the case on display is one of those updated.
     * The callers used to work this out from outside, asking the panel three
     * questions in a row; whether a refresh is needed is the panel's own business.
     */
    public void refreshIfShowing(final @NotNull Collection<TestCaseDto> updated) {
        final TestCaseDto current = getCurrentTestCaseDto();
        if (current == null) return;

        if (updated.stream().anyMatch(item -> item.getId().equals(current.getId())))
            refreshCurrentView();
    }

    public @Nullable TestCaseDto getCurrentTestCaseDto() {
        return page.getCurrentItem();
    }

    public void focusDetailsTab() {
        this.selectContent(ViewTab.DETAILS);
        detailsTab.setFocusable(true);
        detailsTab.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        detailsTab.removeAll();
        historyTab.removeAll();
        openBugsTab.removeAll();

        // Otherwise the static reference keeps this panel (and its project) alive
        // after the project is closed, and other projects would operate on it.
        ViewToolWindowFactory.onPanelDisposed(this);
    }
}