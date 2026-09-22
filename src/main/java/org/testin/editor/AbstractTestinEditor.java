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

package org.testin.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.actions.Declared;
import org.testin.actions.EscapeAction;
import org.testin.editor.grid.GridEnterAction;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.grid.GridView;
import org.testin.editor.list.ListPanelBuilder;
import org.testin.editor.list.ListView;
import org.testin.editor.listeners.GridContextMenuListener;
import org.testin.editor.listeners.GridSelectionListener;
import org.testin.editor.statusbar.PageAction;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.Toolbar;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.ToolBarAttribute;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.open.OpenContextMenuAction;
import org.testin.services.Services;
import org.testin.services.TestCaseValues;
import org.testin.ui.FontSync;
import org.testin.undo.UndoHistories;
import org.testin.undo.UndoScope;
import org.testin.util.Bundle;
import org.testin.util.FailureText;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractTestinEditor<A extends Enum<A> & ToolBarAttribute, N extends DirectoryDto> implements Disposable, Toolbar, TestinEditor {
    @Getter
    protected final @NotNull Project p;

    @Getter
    protected final @NotNull N parent;

    @Getter
    protected final @NotNull List<TestCaseDto> allTestCases;

    @Getter
    protected final @NotNull List<TestCaseDto> currentTestCases;

    protected final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    protected final @NotNull Disposable projectDisposable;
    protected final @NotNull JBPanel<?> mainPanel;
    protected final @NotNull EditorCenter center;

    @Getter
    protected final @NotNull JBList<TestCaseDto> list;

    protected final @NotNull CollectionListModel<TestCaseDto> model;
    protected final @NotNull JBScrollPane scrollPane;

    protected final @NotNull ListView listView;

    protected final @NotNull AbstractEditorContextMenu contextMenu;
    @Getter
    protected final @NotNull StatusBar statusBar = new StatusBar();
    protected @NotNull Optional<GridView> grid = Optional.empty();
    @Getter
    @Setter
    protected int currentPage = 1;
    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-222
    @Getter
    protected int pageSize = TestinEditor.pageSizeOf(PropertiesComponent.getInstance().getValue(TestinEditor.PAGE_SIZE_KEY, ""));
    @Getter
    @Setter
    protected @NotNull String hoveredIconAction = "";
    @Getter
    @Setter
    protected int hoveredIndex = -1;
    protected @NotNull Optional<UUID> selectionToRestore = Optional.empty();
    protected int gridColumnToRestore = -1;
    private boolean goingTo = false;
    private boolean disposed;

    protected AbstractTestinEditor(final @NotNull Project p, final @NotNull N parent) {
        this.p = p;
        this.parent = parent;

        final @NotNull Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.center = new EditorCenter(this.mainPanel);
        this.mainPanel.setBackground(UIUtil.getPanelBackground());
        this.mainPanel.setOpaque(true);

        this.listView = ListPanelBuilder.build(p, projectDisposable, this);
        this.model = listView.model();
        this.list = listView.list();
        this.scrollPane = listView.scrollPane();

        this.contextMenu = buildContextMenu();
    }

    public abstract @NotNull AbstractToolbarPanel getToolBar();

    // UC-EDITOR-PANEL-023, Rule-EDITOR-PANEL-107, Rule-EDITOR-PANEL-222
    @Override
    public void choosePageSize(final int size) {
        pageSize = size;
        PropertiesComponent.getInstance().setValue(TestinEditor.PAGE_SIZE_KEY, size, TestinEditor.DEFAULT_PAGE_SIZE);
    }

    protected abstract @NotNull AbstractEditorContextMenu buildContextMenu();

    protected abstract @NotNull Class<A> attributeType();

    @Override
    public abstract @NotNull Set<A> getSelectedDetails();

    protected abstract @NotNull List<TestCaseDto> getFilteredList();

    protected abstract void loadDataAsync(final @NotNull Runnable onLoaded);

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        if (getToolBar().getCurrentView() != ViewMode.GRID_VIEW) return list;

        return grid.<JComponent>map(GridView::table).orElse(list);
    }

    @Override
    public @NotNull Project getProject() {
        return p;
    }

    @Override
    public @NotNull DirectoryDto getEditedNode() {
        return parent;
    }

    @Override
    public int getShownItemsCount() {
        return currentTestCases.size();
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(currentTestCases);
    }

    protected int getTotalPages(final @NotNull List<TestCaseDto> filtered) {
        return PageWindow.of(filtered.size(), currentPage, pageSize).totalPages();
    }

    protected @NotNull List<TestCaseDto> getCurrentPageItems() {
        final @NotNull PageWindow page = PageWindow.of(currentTestCases.size(), currentPage, pageSize);

        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-095
    @Override
    public @NotNull Set<String> getAvailableGroups() {
        return Services.getInstance(p, TestCaseValues.class).getGroups();
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-119
    @Override
    public boolean isBusy() {
        return grid.map(GridView::isCellOpen).orElse(false);
    }

    protected void refreshCards() {
        model.allContentsChanged();
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-092
    @Override
    public void onToolBarSearchValueChanged() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-019, Rule-EDITOR-PANEL-093
    @Override
    public void onToolBarSearchFocusReleased() {
        list.requestFocusInWindow();
    }

    // UC-EDITOR-PANEL-020, Rule-EDITOR-PANEL-097
    @Override
    public void onToolBarFilterSelectionChanged() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-021, Rule-EDITOR-PANEL-099
    @Override
    public void onToolBarFilterResetButtonClicked() {
        currentPage = 1;
        refreshView();
    }

    // UC-EDITOR-PANEL-003, Rule-EDITOR-PANEL-021
    @Override
    public void onToolBarDetailsSelectionChanged() {
        Logger.debug("[details] selectedDetails changed -> " + getSelectedDetails());

        if (getToolBar().getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[details] grid active -> toggling column visibility");
            updateGridColumns();
        } else {
            refreshCards();
        }
    }

    protected void updateGridColumns() {
        grid.ifPresent(view -> gridPanelBuilder.applyColumnVisibility(view.table(), attributeType(), getSelectedDetails()));
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-018
    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + getToolBar().getCurrentView());
        center.set(scrollPane);

        refreshCards();
    }

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-017
    @Override
    public void onToolBarSwitchedToGridView() {
        Logger.debug("[switch] -> GRID view, currentView=" + getToolBar().getCurrentView());
        rebuildGrid();
        grid.ifPresent(view -> {
            center.set(view.scrollPane());
            ApplicationManager.getApplication().invokeLater(view.table()::requestFocusInWindow);
        });
    }

    protected abstract @NotNull JBTable buildTable(final @NotNull List<TestCaseDto> pageItems, final @NotNull Set<A> attributes);

    protected abstract void installEditListener(final @NotNull JBTable table, final @NotNull List<TestCaseDto> pageItems);

    // UC-EDITOR-PANEL-002, Rule-EDITOR-PANEL-017
    protected void rebuildGrid() {
        final boolean keepKeyboard = grid.map(GridView::handOver).orElse(false);

        final @NotNull List<TestCaseDto> pageItems = getCurrentPageItems();
        final @NotNull Set<A> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        final @NotNull Disposable fontSync = Disposer.newDisposable(projectDisposable, "testin." + getClass().getSimpleName() + ".gridFontSync");
        try {
            final @NotNull JBTable table = buildTable(pageItems, attributes);
            FontSync.syncWithNativeEditor(p, table, fontSync, _ -> GridPanelBuilder.resizeToFont(table));

            table.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, table, list, pageItems));
            installEditListener(table, pageItems);
            new EscapeAction(p, table);
            new GridEnterAction(p, table, pageItems, parent.getPath2());
            table.addMouseListener(new GridContextMenuListener(table, list, contextMenu, pageItems));
            contextMenu.bindShortcutsTo(table);
            PageAction.bindToGrid(this, table);
            new OpenContextMenuAction(table, contextMenu);

            final @NotNull Optional<GridView> previous = grid;
            grid = Optional.of(GridPanelBuilder.finishRebuild(table, list, pageItems, gridColumnToRestore, fontSync, keepKeyboard));

            previous.ifPresent(old -> Disposer.dispose(old.fontSync()));

            gridColumnToRestore = -1;
            Logger.debug("[grid] rebuildGrid done, rows=" + table.getRowCount() + ", cols=" + table.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
            Disposer.dispose(fontSync);

            // Rule-EDITOR-PANEL-229
            grid.ifPresent(old -> Disposer.dispose(old.fontSync()));
            grid = Optional.empty();
            center.set(scrollPane);
            Services.getInstance(p, Notifier.class).softRefuse(p, Bundle.message("editor.grid.not.drawn", FailureText.of(ex)));
        }
    }

    protected void loadDataAsync() {
        loadDataAsync(() -> {
        });
    }

    @Override
    public void reloadData() {
        reloadData(() -> {
        });
    }

    protected void reloadData(final @NotNull Runnable onLoaded) {
        beforeReload();

        rememberSelection();

        allTestCases.clear();
        currentTestCases.clear();
        clearLoadedData();

        model.removeAll();
        list.setPaintBusy(true);
        list.getEmptyText().setText(Bundle.message("editor.refreshing"));

        loadDataAsync(onLoaded);
    }

    protected void beforeReload() {
    }

    protected void clearLoadedData() {
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-117
    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + getToolBar().getCurrentView());

        final @NotNull Done message = refreshed();

        reloadData(() -> Services.getInstance(p, Notifier.class).softShow(p, message));

        Services.getInstance(p, TestCaseValues.class).reload(Services.getInstance(p, ProjectIndexer.class)::getAllTestCases);
    }

    protected @NotNull Done refreshed() {
        return Done.REFRESHED;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-104, Rule-EDITOR-PANEL-009
    @Override
    public void selectWhenLoaded(final @NotNull UUID id) {
        final @NotNull Optional<TestCaseDto> loaded;
        synchronized (allTestCases) {
            loaded = allTestCases.stream()
                    .filter(tc -> id.equals(tc.getId()))
                    .findFirst();
        }

        if (loaded.isPresent()) {
            selectTestCase(loaded.get());
            return;
        }

        selectionToRestore = Optional.of(id);
        goingTo = true;
    }

    protected void focusIfGoingTo() {
        if (!goingTo) return;

        goingTo = false;
        list.requestFocusInWindow();
    }

    // UC-EDITOR-PANEL-025, Rule-EDITOR-PANEL-009
    @Override
    public void selectTestCase(final @NotNull TestCaseDto tc) {
        final int index = currentTestCases.indexOf(tc);
        if (index < 0) {
            notOnAnyPage(tc);
            return;
        }

        final int safePageSize = Math.max(1, pageSize);
        final int page = (index / safePageSize) + 1;
        final int localIndex = index % safePageSize;

        if (page == currentPage) {
            selectVisibleIndex(localIndex);
            return;
        }

        currentPage = page;
        refreshView();

        ApplicationManager.getApplication().invokeLater(() -> selectVisibleIndex(localIndex));
    }

    protected void notOnAnyPage(final @NotNull TestCaseDto tc) {
    }

    protected void selectVisibleIndex(final int index) {
        if (index < 0 || index >= list.getModel().getSize()) return;

        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        list.requestFocusInWindow();
    }

    protected void rememberSelection() {
        selectionToRestore = Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId);
    }

    protected void wireList() {
        PageAction.bindTo(this, list);
        Declared.bindTo("Testin.ViewDetails", list);
        Declared.bindTo("Testin.CopyTestCase", list);
        Declared.bindTo("Testin.RemoveTestCase", list);

        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> grid.map(GridView::table),
                () -> getToolBar().getCurrentView() == ViewMode.GRID_VIEW);
    }

    protected boolean isDisposed() {
        return disposed;
    }

    // UC-EDITOR-PANEL-022, Rule-EDITOR-PANEL-101
    @Override
    public void refreshView() {
        if (disposed) return;

        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final int totalItems = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        currentPage = page.page();

        final @NotNull List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        final @NotNull Optional<UUID> selectedId = selectionToRestore
                .or(() -> Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));

        replaceModel(pageItems);

        selectedId.ifPresent(id -> {
            for (final TestCaseDto item : pageItems) {
                if (id.equals(item.getId())) {
                    list.setSelectedValue(item, true);
                    break;
                }
            }
        });
        selectionToRestore = Optional.empty();

        drawStatus(page, totalItems);

        refreshSelectionStatus(list.getSelectedIndices());
        afterSelectionShown();

        if (getToolBar().getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            grid.ifPresent(view -> center.set(view.scrollPane()));
        }

        afterViewRefreshed();
    }

    protected void replaceModel(final @NotNull List<TestCaseDto> pageItems) {
        model.replaceAll(pageItems);
    }

    protected abstract void drawStatus(final @NotNull PageWindow page, final int totalItems);

    protected void afterSelectionShown() {
    }

    // Rule-EDITOR-PANEL-135
    protected void afterViewRefreshed() {
    }

    @Override
    public void dispose() {
        disposed = true;

        teardown(
                this::beforeDispose,

                () -> Disposer.dispose(projectDisposable),

                this::stopListening,
                () -> getToolBar().dispose(),

                () -> Services.getInstance(p, UndoHistories.class).forget(UndoScope.of(parent.getPath())),

                allTestCases::clear,
                currentTestCases::clear,
                this::disposeLoadedData,

                model::removeAll,
                mainPanel::removeAll,

                TestinEditor.super::dispose);

        Logger.debug("dispose " + getClass().getSimpleName() + ": " + parent.getName() + " - " + parent.getPath());
    }

    protected final void teardown(final @NotNull Runnable... steps) {
        for (final Runnable step : steps) {
            try {
                step.run();
            } catch (final Exception ex) {
                Logger.warn("Teardown step failed in " + getClass().getSimpleName() + ", the rest still ran: " + ex);
            }
        }
    }

    private void stopListening() {
        for (final MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);
    }

    protected void beforeDispose() {
    }

    protected void disposeLoadedData() {
    }

    // UC-EDITOR-PANEL-027, Rule-EDITOR-PANEL-118
    protected void jumpToPageOfPendingSelection() {
        final int page = selectionToRestore
                .map(id -> PageWindow.pageContaining(id, currentTestCases, pageSize))
                .orElse(0);

        if (page == 0) selectionToRestore = Optional.empty();
        else currentPage = page;
    }
}
