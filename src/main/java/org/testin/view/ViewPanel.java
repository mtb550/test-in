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
import org.testin.indexer.ProjectIndexer;
import org.testin.actions.EscapeAction;
import org.testin.editor.WheelForwarding;
import org.testin.services.Services;
import org.testin.model.dto.TestCaseDto;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.ui.FontSync;
import org.testin.view.bugs.OpenBugsTab;
import org.testin.view.details.DetailsTab;
import org.testin.view.history.HistoryTab;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
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

        // Rule-SETTING-039. The other half of the wheel, beside the zoom that
        // takes it: a tab with a wheel listener receives the wheel itself instead
        // of the scroll pane around it, so a plain wheel scrolled none of the
        // three tabs (#312, A75).
        tabs().forEach(tab -> tab.addMouseWheelListener(WheelForwarding::forwardWheelToScrollPane));

        detailsScrollPane = createScrollPane(detailsTab);
        historyScrollPane = createScrollPane(historyTab);
        openBugsScrollPane = createScrollPane(openBugsTab);

        // Rule-VIEW-PANEL-058. On each tab, because focus is in whichever one
        // the tester is reading - and F2 puts it there, which is how a tester
        // ended up unable to close the panel with the key that closes it
        // everywhere else (#226).
        new EscapeAction(p, detailsTab);
        new EscapeAction(p, historyTab);
        new EscapeAction(p, openBugsTab);

        tabs().forEach(this::takesTheKeyboard);
        IdeEventQueue.getInstance().addPostprocessor(this::focusTabPressed, this);

        page = new ViewPagination(this);

        refreshCurrentView();

        TestCaseExecutionSubscriber.onReported(p, this, (tc, status, duration, failure) -> refreshCurrentView());
    }

    private @NotNull Stream<JBPanel<?>> tabs() {
        return Stream.of(detailsTab, historyTab, openBugsTab);
    }

    /**
     * UC-VIEW-PANEL-017, Rule-VIEW-PANEL-079, Rule-VIEW-PANEL-080.
     * <p>
     * A tab the keyboard can be in: focusable from the start, and answering
     * {@code Tab} and {@code Shift+Tab} (#311).
     * <p>
     * {@code Tab} is a focus traversal key, and AWT hands it to the focus manager
     * before the IDE's actions are asked, so the tab stops treating it as one -
     * otherwise the two actions never run.
     */
    private void takesTheKeyboard(final @NotNull JBPanel<?> tab) {
        tab.setFocusable(true);
        tab.setFocusTraversalKeysEnabled(false);

        new ViewTabAction(p, tab, ViewTabAction.Direction.NEXT);
        new ViewTabAction(p, tab, ViewTabAction.Direction.PREVIOUS);
    }

    /**
     * UC-VIEW-PANEL-017, Rule-VIEW-PANEL-080.
     * <p>
     * A press anywhere inside a tab puts the keyboard in that tab (#66, finding
     * 158).
     * <p>
     * Watched on the IDE's event queue rather than by a listener on each tab.
     * Nearly everything a tab draws is a {@code Prose} text area, and a text area
     * takes the press for its caret before any parent hears it, so a listener on
     * the tab heard only the empty space between rows. Through the focus manager,
     * so a press that comes back from another window lands too. Never consumes
     * the press: the link or path step under it still does its own thing.
     */
    private boolean focusTabPressed(final @NotNull AWTEvent event) {
        if (!(event instanceof MouseEvent press) || press.getID() != MouseEvent.MOUSE_PRESSED) return false;

        // The queue sees the press before Swing hands it down, so its component
        // is the IDE's frame (IdeFrameImpl, measured in the sandbox) and never a
        // tab. What was pressed is the deepest component under the pointer.
        Optional.ofNullable(SwingUtilities.getDeepestComponentAt(press.getComponent(), press.getX(), press.getY()))
                .flatMap(pressed -> tabs().filter(tab -> SwingUtilities.isDescendingFrom(pressed, tab)).findFirst())
                .ifPresent(tab -> IdeFocusManager.getInstance(p).requestFocus(tab, true));
        return false;
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

    /**
     * UC-VIEW-PANEL-002, Rule-VIEW-PANEL-015, Rule-VIEW-PANEL-016.
     * <p>
     * Follows what is selected, and stays shut when the tester has shut it.
     * <p>
     * The difference between this and {@link #show} is who asked. Double-clicking
     * a card or pressing ENTER on a sequence cell is a request for the details,
     * and opens the panel. Changing the selection, or moving to another editor,
     * is not - it is the tester doing something else, and a panel they closed
     * must not reappear because they clicked a different tab.
     * <p>
     * The rule was in one of the two places that needed it. Selection changes
     * checked; switching editors never did, so closing the panel lasted until the
     * next editor tab.
     */
    public void showIfOpen(final @NotNull List<TestCaseDto> testCases, final @NotNull List<String> path) {
        if (!isOpen()) return;

        this.show(testCases, path);
    }

    /**
     * Whether the tester has this panel on screen. Asked by everything that
     * follows rather than asks - three places were spelling the same question
     * out, which is two more than can be changed together.
     */
    private boolean isOpen() {
        return ViewToolWindowFactory.toolWindow(p).filter(ToolWindow::isVisible).isPresent();
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
     * UC-VIEW-PANEL-015, Rule-VIEW-PANEL-060.
     * <p>
     * Closes the panel when what it is showing came from the node being closed
     * - an editor shutting down takes its own cases off the screen, and nobody
     * else's.
     * <p>
     * By the node rather than by one case. It used to be told whichever case
     * happened to be selected as the editor went down, and the editor's own
     * teardown emptied the panel unconditionally a moment later anyway - so a
     * tester reading a case from one editor watched the panel go blank because
     * they closed another (#233).
     */
    public void hide(final @NotNull List<String> closingPath) {
        if (!isOpen()) return;
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
        new DetailsTab().load(p, detailsTab, currentFromIndex(), page.getCurrentPath());
        new HistoryTab().load(historyTab);
        new OpenBugsTab().load(p, openBugsTab, currentFromIndex());
    }

    /**
     * Rule-VIEW-PANEL-005.
     * <p>
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