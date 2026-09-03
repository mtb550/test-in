package org.testin.view;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.indexer.ProjectIndexer;
import org.testin.services.Services;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.util.FontSync;
import org.testin.view.bugs.OpenBugsTab;
import org.testin.view.details.DetailsTab;
import org.testin.view.history.HistoryTab;

import java.awt.*;
import java.util.Collection;
import java.util.Optional;
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

        TestCaseExecutionSubscriber.onReported(p, this, (tc, status, duration, failure) -> refreshCurrentView());
    }

    private @NotNull JBScrollPane createScrollPane(final @NotNull Component view) {
        final @NotNull JBScrollPane sp = new JBScrollPane(view);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public void show(final @NotNull Project p, final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        if (testCases.isEmpty()) return;

        ViewToolWindowFactory.toolWindow(p).ifPresent(tw -> tw.show(() -> {
            selectDetailsTab();
            this.updateList(testCases, path);
        }));
    }

    public void show(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        this.show(p, testCases, path);
    }

    public @NotNull ViewPanel hide() {
        ViewToolWindowFactory.toolWindow(p)
                .filter(ToolWindow::isVisible)
                .ifPresent(tw -> tw.hide(null));
        return this;
    }

    public void reset() {
        this.updateList(List.of(), List.of());
    }

    /**
     * Brings the Details tab to the front. Both callers show a test case, and a
     * test case is shown on Details - no other tab was ever asked for.
     */
    private void selectDetailsTab() {
        ViewToolWindowFactory.toolWindow(p).ifPresent(tw -> {
            for (final Content content : tw.getContentManager().getContents()) {
                if (ViewTab.DETAILS.getDisplayName().equals(content.getDisplayName())) {
                    tw.getContentManager().setSelectedContent(content);
                    break;
                }
            }
        });
    }

    /**
     * Closes the panel when what it is showing is the case being closed
     * elsewhere - an editor shutting down takes its own case off the screen.
     */
    public void hide(final @NotNull TestCaseDto testCaseDtoToMatch) {
        if (ViewToolWindowFactory.toolWindow(p).filter(ToolWindow::isVisible).isEmpty()) return;

        getCurrentTestCase()
                .filter(shown -> shown.getId().equals(testCaseDtoToMatch.getId()))
                .ifPresent(shown -> {
                    this.reset();
                    this.hide();
                });
    }

    public void updateList(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        this.page.updateList(testCases, path);
        this.refreshCurrentView();
    }

    public void refreshCurrentView() {
        new DetailsTab().load(p, detailsTab, currentFromIndex(), page.getCurrentPath());
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
        getCurrentTestCase()
                .filter(current -> updated.stream().anyMatch(item -> item.getId().equals(current.getId())))
                .ifPresent(current -> refreshCurrentView());
    }

    /**
     * The case on display, empty while the panel is showing none.
     */
    public @NotNull Optional<TestCaseDto> getCurrentTestCase() {
        return page.getCurrentItem();
    }

    /**
     * The case on display as the indexer holds it now, rather than as this panel
     * was handed it.
     * <p>
     * The panel is given cases when it is opened and keeps them while the tester
     * pages through - so a redraw drew whatever it was holding, which is the
     * value at the moment the panel opened. Every writer telling the panel to
     * refresh was still not enough: it refreshed, and re-rendered the same stale
     * object.
     * <p>
     * Falls back to the held copy when the indexer no longer has the case - a
     * removal, a project reindexed underneath - because a panel that blanks is
     * worse than one showing the last thing that was true.
     */
    private @NotNull Optional<TestCaseDto> currentFromIndex() {
        return getCurrentTestCase()
                .map(shown -> Services.getInstance(p, ProjectIndexer.class).findTestCase(shown.getId()).orElse(shown));
    }

    public void focusDetailsTab() {
        selectDetailsTab();
        detailsTab.setFocusable(true);
        detailsTab.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        detailsTab.removeAll();
        historyTab.removeAll();
        openBugsTab.removeAll();

        ViewToolWindowFactory.onPanelDisposed(p, this);
    }
}