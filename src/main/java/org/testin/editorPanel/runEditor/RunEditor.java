package org.testin.editorPanel.runEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.EscapeAction;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.PageWindow;
import org.testin.editorPanel.TestCaseFilter;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.grid.GridPanelBuilder;
import org.testin.editorPanel.list.ListPanelBuilder;
import org.testin.editorPanel.list.ListView;
import org.testin.editorPanel.listeners.GridContextMenuListener;
import org.testin.editorPanel.listeners.GridSelectionListener;
import org.testin.editorPanel.listeners.RunListRenderer;
import org.testin.editorPanel.listeners.StatusBarListener;
import org.testin.editorPanel.statusBar.StatusBar;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.editorPanel.toolBar.RunToolBar;
import org.testin.editorPanel.toolBar.components.FilterPopupBtn;
import org.testin.editorPanel.toolBar.components.RunDetailsPopupBtn;
import org.testin.editorPanel.toolBar.components.SearchTxt;
import org.testin.editorPanel.toolBar.components.StartExecutionBtn;
import org.testin.enums.*;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.TestRunItems;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.TestRunDto;
import org.testin.mappers.dto.dirs.TestRunDirectoryDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testCase.TestCaseSorter;
import org.testin.testRun.UpdateTestRunStatusAction;
import org.testin.util.FontSync;
import org.testin.viewPanel.GridViewDetailsAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RunEditor implements Disposable, IToolBar, IEditor {

    private final @NotNull Project p;

    @Getter
    private final @NotNull TestRunDirectoryDto parent;

    @Getter
    private final @NotNull List<TestCaseDto> allTestCases;

    @Getter
    private final @NotNull List<TestCaseDto> currentTestCases;

    @Getter
    private final @NotNull Map<UUID, TestRunItems> resultsMap;

    private final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final @NotNull Disposable projectDisposable;
    private final @NotNull RunExecutionTimer executionTimer = new RunExecutionTimer();
    /**
     * Guards against a stale in-flight load overwriting a newer one (e.g. double refresh).
     */
    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();
    /**
     * Child disposable for the current grid table's font-sync subscription;
     * replaced on every grid rebuild so old subscriptions do not accumulate.
     */
    private @Nullable Disposable gridFontSyncDisposable;
    private @NotNull JBPanel<?> mainPanel;
    private @Nullable JBList<TestCaseDto> list;
    private @Nullable CollectionListModel<TestCaseDto> model;
    private @Nullable JBTable gridTable;
    private @NotNull RunEditorContextMenu contextMenu;
    private @Nullable JBScrollPane gridScrollPane;
    private @NotNull JBScrollPane listScrollPane;
    private @Nullable JComponent currentCenter;
    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize = 50;

    @Getter
    private @NotNull StatusBar statusBar;

    @Getter
    @NotNull
    private AbstractToolbarPanel toolBar;

    @Getter
    @Setter
    private @Nullable String hoveredIconAction = null;

    @Getter
    @Setter
    private int hoveredIndex = -1;
    @Getter
    private volatile @Nullable TestRunDto tr;

    @Getter
    private int currentlyExecutingIndex = -1;

    public RunEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        this.p = p;
        this.parent = vf.getTestRun();

        final Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.resultsMap = new ConcurrentHashMap<>();

        buildOpeningPanel();
        loadDataAsync();
    }

    private void buildOpeningPanel() {
        toolBar = new RunToolBar(p, this);
        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        // Shared list-view construction (see ListPanelBuilder, the counterpart of GridPanelBuilder).
        final ListView listView = ListPanelBuilder.build(p, projectDisposable);
        model = listView.model();
        list = listView.list();
        listScrollPane = listView.scrollPane();

        // Run editor specifics: the run card renderer.
        list.setCellRenderer(new RunListRenderer(p, this));

        this.contextMenu = new RunEditorContextMenu(p, this, parent, list);
        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> gridTable,
                () -> toolBar.getCurrentView() == ViewMode.GRID_VIEW);

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        // List view is the default mode when the editor opens.
        onToolBarSwitchedToListView();

        refreshView();
    }

    private void loadDataAsync() {
        final int generation = loadGeneration.incrementAndGet();
        if (list != null) {
            list.setPaintBusy(true);
            list.getEmptyText().setText("Loading...");
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();
                // Snapshot the volatile field into a local. Re-reading it between
                // the null check and the call would let another thread clear it.
                TestRunDto run = tr;
                if (run == null) {
                    run = indexer.getTestRunByPath(parent.getPath());
                    tr = run;
                }

                if (run != null) {
                    final Map<UUID, TestRunItems> newResults = run.getResults().stream()
                            .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                    (existingItem, duplicateItem) -> existingItem));
                    resultsMap.putAll(newResults);
                }

                final List<TestCaseDto> loadedItems = new ArrayList<>();
                if (run != null) {
                    for (final TestRunItems item : run.getResults()) {
                        final TestCaseDto testCase = indexer.getTestCaseById(item.getId());
                        if (testCase == null) {
                            Logger.warn("Test run references missing test case id=" + item.getId());
                            continue;
                        }
                        loadedItems.add(testCase);
                        final TestRunItems runItem = resultsMap.get(item.getId());
                        if (runItem != null) runItem.setTc(testCase);
                    }
                }

                final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(p, loadedItems).sortedList();
                Services.getInstance(p, TestCaseCacheService.class).load(sorted);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    allTestCases.clear();
                    allTestCases.addAll(sorted);
                    currentTestCases.clear();
                    currentTestCases.addAll(sorted);

                    if (list != null) {
                        list.setPaintBusy(false);
                        if (allTestCases.isEmpty()) {
                            list.getEmptyText().setText("No test cases found in this run.");
                        }
                    }
                    final StartExecutionBtn startBtn = toolBar.getToolbarItem(StartExecutionBtn.class);
                    if (startBtn != null) startBtn.updateEnabledState();
                    refreshView();
                });
            } catch (final Exception ex) {
                Logger.error("Failed to load Test Run data from disk: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (list != null) {
                        list.setPaintBusy(false);
                        list.getEmptyText().setText("Unable to load this test run.");
                    }
                });
            }
        });
    }

    @Override
    public void onToolBarSearchValueChanged(final @NotNull String query) {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarSearchFocusReleased() {
        if (list != null) list.requestFocusInWindow();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    /**
     * Refreshes status-dependent filtering without losing the page the user is viewing.
     */
    public void refreshAfterStatusChange() {
        final int pageBeforeRefresh = currentPage;
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = pageBeforeRefresh;
        refreshView();
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarDetailsSelectionChanged() {
        Logger.debug("[details] selectedDetails changed -> " + getSelectedDetails());
        if (list != null && model != null) {
            model.allContentsChanged();
        }
        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[details] grid active -> toggling column visibility");
            updateGridColumns();
        }
    }

    private void updateGridColumns() {
        if (gridTable == null) return;
        gridPanelBuilder.applyColumnVisibility(gridTable, Arrays.stream(RunEditorAttributes.values()).toList(), RunEditorAttributes::getName, getSelectedDetails());
    }

    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + toolBar.getCurrentView());
        setCenter(listScrollPane);
    }

    @Override
    public void onToolBarSwitchedToGridView() {
        Logger.debug("[switch] -> GRID view, currentView=" + toolBar.getCurrentView());
        rebuildGrid();
        // rebuildGrid() swallows failures; the grid parts are then still null
        // and the previous center stays visible instead of an NPE.
        if (gridScrollPane != null) setCenter(gridScrollPane);
        if (gridTable != null) SwingUtilities.invokeLater(gridTable::requestFocusInWindow);
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());
        final FilterPopupBtn toolBarFilter = toolBar.getToolbarItem(FilterPopupBtn.class);
        if (toolBarFilter != null)
            toolBarFilter.clearFilters();

        final SearchTxt toolBarSearch = toolBar.getToolbarItem(SearchTxt.class);
        if (toolBarSearch != null)
            toolBarSearch.resetSearchQuery();

        this.allTestCases.clear();
        this.currentTestCases.clear();
        this.resultsMap.clear();

        this.tr = null;

        if (this.model != null)
            this.model.removeAll();

        if (this.list != null) {
            this.list.setPaintBusy(true);
            this.list.getEmptyText().setText("Refreshing...");
        }

        loadDataAsync();
    }

    @Override
    public @NotNull Set<RunEditorAttributes> getSelectedDetails() {
        final AbstractToolbarPanel baseToolBar = getToolBar();
        final RunDetailsPopupBtn popup = baseToolBar.getToolbarItem(RunDetailsPopupBtn.class);
        if (popup != null) {
            return popup.getSelectedDetails();
        }
        return Collections.emptySet();
    }

    @Override
    public int getTotalPageCount() {
        return PageWindow.of(currentTestCases.size(), currentPage, pageSize).totalPages();
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public void appendNewTestCase(final @NotNull TestCaseDto tc) {
        this.allTestCases.add(tc);
        refreshView();
    }

    public void refreshView() {
        final int total = currentTestCases.size();
        final PageWindow page = PageWindow.of(total, currentPage, pageSize);
        currentPage = page.page();
        // Copy: listeners retain this list, and a live subList view would throw
        // ConcurrentModificationException after currentTestCases is next mutated.
        final List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        final TestCaseDto selectedItem = list != null ? list.getSelectedValue() : null;

        if (model != null) {
            model.replaceAll(pageItems);
        }

        if (selectedItem != null && pageItems.contains(selectedItem) && list != null) {
            list.setSelectedValue(selectedItem, true);
        }

        statusBar.updatePaginationState(page.page(), page.totalPages(), total);

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            if (gridScrollPane != null) setCenter(gridScrollPane);
        }
    }

    private @NotNull List<TestCaseDto> getCurrentPageItems() {
        final int total = currentTestCases.size();
        final PageWindow page = PageWindow.of(total, currentPage, pageSize);
        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    private void rebuildGrid() {
        final List<TestCaseDto> pageItems = getCurrentPageItems();
        final Set<RunEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            gridTable = gridPanelBuilder.buildRunTable(p, pageItems, attributes, resultsMap, (currentPage - 1) * pageSize);

            if (gridFontSyncDisposable != null) Disposer.dispose(gridFontSyncDisposable);
            gridFontSyncDisposable = Disposer.newDisposable(projectDisposable, "testin.runEditor.gridFontSync");
            FontSync.syncWithNativeEditor(p, gridTable, gridFontSyncDisposable);

            gridTable.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, gridTable, pageItems));
            // ESC in grid view behaves like ESC in the list: hide the view panel, then clear the selection.
            new EscapeAction(p, gridTable);
            // ENTER on the non-editable sequence column opens the details view.
            new GridViewDetailsAction(p, gridTable, pageItems, parent.getPath2()).installDoubleClick();
            // Read the nullable field once: the context menu listener needs a real
            // list, and the selection carried over from list view comes from it.
            final JBList<TestCaseDto> currentList = list;
            if (currentList != null) {
                gridTable.addMouseListener(new GridContextMenuListener(gridTable, currentList, contextMenu, pageItems));

                final TestCaseDto selectedItem = currentList.getSelectedValue();
                final int selectedRow = pageItems.indexOf(selectedItem);
                if (selectedRow >= 0) {
                    gridTable.changeSelection(selectedRow, 0, false, false);
                }
            }

            gridScrollPane = new JBScrollPane(gridTable);
            Logger.debug("[grid] rebuildGrid done, rows=" + gridTable.getRowCount() + ", cols=" + gridTable.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
    }

    private void setCenter(final @NotNull JComponent component) {
        Logger.debug("[center] setCenter -> " + component.getClass().getSimpleName()
                + " (had center=" + (currentCenter != null) + ")");
        if (currentCenter != null) {
            mainPanel.remove(currentCenter);
        }
        mainPanel.add(component, BorderLayout.CENTER);
        currentCenter = component;
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @Override
    public @NotNull Set<String> getAvailableModules() {
        final Set<String> modules = new HashSet<>();
        for (final TestCaseDto tc : allTestCases) {
            final String module = tc.getModule();
            if (!module.trim().isEmpty()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private @NotNull List<TestCaseDto> getFilteredList() {
        final String query = toolBar.getSearchTxt().getSearchQuery();

        final FilterPopupBtn filterPopup = toolBar.getToolbarItem(FilterPopupBtn.class);

        final Set<Group> groupFilter = filterPopup != null ? filterPopup.getSelectedGroup() : Collections.emptySet();
        final Set<Priority> priorityFilter = filterPopup != null ? filterPopup.getSelectedPriority() : Collections.emptySet();
        final Set<String> moduleFilter = filterPopup != null ? filterPopup.getSelectedModule() : Collections.emptySet();
        final Set<TestStatus> statusFilter = filterPopup != null ? filterPopup.getSelectedStatus() : Collections.emptySet();

        synchronized (allTestCases) {
            return TestCaseFilter.filter(
                    allTestCases,
                    query,
                    groupFilter,
                    priorityFilter,
                    moduleFilter,
                    statusFilter,
                    resultsMap::get);
        }
    }

    @Override
    public void dispose() {
        // Releases the message-bus subscriptions (font sync) registered against this editor's lifetime.
        Disposer.dispose(projectDisposable);

        executionTimer.dispose();
        if (list != null)
            for (final MouseListener listener : list.getMouseListeners())
                list.removeMouseListener(listener);

        toolBar.dispose();
        statusBar.dispose();

        allTestCases.clear();
        resultsMap.clear();
        if (model != null) model.removeAll();
        mainPanel.removeAll();
        IEditor.super.dispose();

        Logger.debug("dispose run editor: " + parent.getName() + " - " + parent.getPath());

    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return list != null ? list : mainPanel;
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void updateSequenceAndSaveAll() {
    }

    @Override
    public void selectTestCase(final @Nullable TestCaseDto tc) {
        if (tc == null) return;

        final int index = currentTestCases.indexOf(tc);
        if (index < 0 || list == null) return;

        final int targetPage = (index / Math.max(1, pageSize)) + 1;
        final int localIndex = index % Math.max(1, pageSize);
        if (targetPage != currentPage) {
            currentPage = targetPage;
            refreshView();
            ApplicationManager.getApplication().invokeLater(() -> selectVisibleIndex(localIndex));
            return;
        }
        selectVisibleIndex(localIndex);
    }

    private void selectVisibleIndex(final int index) {
        if (list == null || index < 0 || index >= list.getModel().getSize()) return;
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        list.requestFocusInWindow();
    }

    @Override
    public @NotNull Set<UUID> getUnsortedIds() {
        return Collections.emptySet();
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list != null ? list.getSelectedValuesList() : Collections.emptyList();
    }

    public void startTimerForIndex(final int globalIndex) {
        if (globalIndex >= currentTestCases.size()) {
            final UpdateTestRunStatusAction changeStatus = new UpdateTestRunStatusAction(p, this, list);
            changeStatus.onExecutionFinished(p, this);
            return;
        }

        currentlyExecutingIndex = globalIndex;

        final int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        final int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        if (list != null) {
            list.setSelectedIndex(localIndex);
            list.ensureIndexIsVisible(localIndex);
        }

        final TestCaseDto currentTc = currentTestCases.get(globalIndex);
        final TestRunItems runItem = resultsMap.get(currentTc.getId());

        if (runItem == null) {
            // No run data for this case; skip it. Status changes themselves go
            // through RunStatusService, which owns advance + persist.
            startTimerForIndex(globalIndex + 1);
            return;
        }

        executionTimer.start(runItem, () -> {
            if (list != null) list.repaint();
        });
    }

    public void stopExecution() {
        executionTimer.stop();
        currentlyExecutingIndex = -1;
    }

    @Override
    public void onStartExecutionClicked() {
        final UpdateTestRunStatusAction changeStatus = new UpdateTestRunStatusAction(p, this, list);
        changeStatus.applyStatusChange(p, this, TestRunStatus.IN_PROGRESS);
        startTimerForIndex(0);
    }


}
