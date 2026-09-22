/*
 * Copyright 2026 Muteb Almughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.view;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.EscapeAction;
import org.testin.editor.WheelForwarding;
import org.testin.indexer.ProjectIndexer;
import org.testin.model.TestRunItems;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.setting.TestinRoot;
import org.testin.ui.FontSync;

import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ViewPanel implements Disposable {
    @Getter
    private final @NotNull JBPanel<?> detailsTab;
    @Getter
    private final @NotNull JBPanel<?> historyTab;
    @Getter
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

        // Rule-SETTING-039
        tabs().forEach(tab -> tab.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane));

        detailsScrollPane = createScrollPane(detailsTab);
        historyScrollPane = createScrollPane(historyTab);
        openBugsScrollPane = createScrollPane(openBugsTab);

        // Rule-VIEW-PANEL-058
        new EscapeAction(p, detailsTab);
        new EscapeAction(p, historyTab);
        new EscapeAction(p, openBugsTab);

        tabs().forEach(this::takesTheKeyboard);
        IdeEventQueue.getInstance().addPostprocessor(event -> {
            focusTabPressed(event);
            return false;
        }, this);

        page = new ViewPagination(this);

        refreshCurrentView();

        TestCaseExecutionSubscriber.onReported(p, this, (tc, status, duration, failure) -> refreshIfShowing(List.of(tc)));
    }

    private @NotNull Stream<JBPanel<?>> tabs() {
        return Stream.of(detailsTab, historyTab, openBugsTab);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080
    private void takesTheKeyboard(final @NotNull JBPanel<?> tab) {
        tab.setFocusable(true);
        tab.setFocusTraversalKeysEnabled(false);

        new ViewTabAction(p, tab, ViewTabAction.Direction.NEXT);
        new ViewTabAction(p, tab, ViewTabAction.Direction.PREVIOUS);
    }

    // UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080
    private void focusTabPressed(final @NotNull AWTEvent event) {
        if (!(event instanceof MouseEvent press) || press.getID() != MouseEvent.MOUSE_PRESSED) return;

        Optional.ofNullable(SwingUtilities.getDeepestComponentAt(press.getComponent(), press.getX(), press.getY()))
                .flatMap(pressed -> tabs().filter(tab -> SwingUtilities.isDescendingFrom(pressed, tab)).findFirst())
                .ifPresent(tab -> IdeFocusManager.getInstance(p).requestFocus(tab, true));
    }

    private @NotNull JBScrollPane createScrollPane(final @NotNull Component view) {
        final @NotNull JBScrollPane sp = new JBScrollPane(view);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-010, Rule-VIEW-PANEL-012
    public void show(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        if (testCases.isEmpty()) return;

        ViewToolWindowFactory.toolWindow(p).ifPresent(tw -> tw.show(() -> {
            selectDetailsTab();
            this.updateList(testCases, path);
        }));
    }

    // UC-VIEW-PANEL-002, Rule-VIEW-PANEL-015, Rule-VIEW-PANEL-016
    public void showIfOpen(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        if (isClosed()) return;

        this.show(testCases, path);
    }

    private boolean isClosed() {
        return ViewToolWindowFactory.toolWindow(p).filter(ToolWindow::isVisible).isEmpty();
    }

    // UC-VIEW-PANEL-015
    public @NotNull ViewPanel hide() {
        ViewToolWindowFactory.toolWindow(p)
                .filter(ToolWindow::isVisible)
                .ifPresent(ToolWindow::hide);
        return this;
    }

    // UC-VIEW-PANEL-002, Rule-VIEW-PANEL-017
    public void reset() {
        this.updateList(List.of(), List.of());
    }

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

    // UC-VIEW-PANEL-015, Rule-VIEW-PANEL-060
    public void hide(final @NotNull List<String> closingPath) {
        if (isClosed()) return;
        if (!page.getCurrentPath().equals(closingPath)) return;

        this.reset();
        this.hide();
    }

    public void updateList(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        this.page.updateList(testCases, path);
        this.refreshCurrentView();
    }

    // Rule-VIEW-PANEL-008
    public void refreshCurrentView() {
        for (final ViewTab tab : ViewTab.values()) tab.load(this);
    }

    // Rule-VIEW-PANEL-005
    public void refreshIfShowing(final @NotNull Collection<TestCaseDto> updated) {
        getCurrentTestCase()
                .filter(current -> updated.stream().anyMatch(item -> item.getId().equals(current.getId())))
                .ifPresent(current -> refreshCurrentView());
    }

    public @NotNull Optional<TestCaseDto> getCurrentTestCase() {
        return page.getCurrentItem();
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-083
    @NotNull Optional<TestCaseDto> shownCase() {
        return getCurrentTestCase().map(shown -> shownRunItem().map(TestRunItems::shownCase)
                .orElseGet(() -> Services.getInstance(p, ProjectIndexer.class).findTestCase(shown.getId()).orElse(shown)));
    }

    // UC-VIEW-PANEL-004, Rule-VIEW-PANEL-083
    @NotNull Optional<TestRunItems> shownRunItem() {
        final @NotNull List<String> path = page.getCurrentPath();
        if (path.isEmpty()) return Optional.empty();

        return getCurrentTestCase().flatMap(shown -> Services.getInstance(p, ProjectIndexer.class)
                .findTestRun(Services.getInstance(p, TestinRoot.class).resolve(path))
                .flatMap(run -> run.resultOf(shown.getId())));
    }

    // UC-VIEW-PANEL-001, Rule-VIEW-PANEL-011, Rule-VIEW-PANEL-012
    public void focusDetailsTab() {
        selectDetailsTab();
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