package org.testin.editor.run;

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
import org.testin.editor.*;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.list.ListPanelBuilder;
import org.testin.editor.list.ListView;
import org.testin.editor.listeners.GridContextMenuListener;
import org.testin.editor.listeners.GridSelectionListener;
import org.testin.editor.listeners.RunListRenderer;
import org.testin.editor.listeners.StatusBarListener;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.RunToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.RunDetailsPopupBtn;
import org.testin.editor.toolbar.components.StartExecutionBtn;
import org.testin.editor.toolbar.components.StopExecutionBtn;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.RunEditorAttributes;
import org.testin.model.TestRunItems;
import org.testin.model.TestRunStatus;
import org.testin.model.TestStatus;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Notifier;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.TestCaseSorter;
import org.testin.testrun.UpdateTestRunStatusAction;
import org.testin.util.FontSync;
import org.testin.util.Tools;
import org.testin.view.GridViewDetailsAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RunEditor implements Disposable, Toolbar, TestinEditor {

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
    private @NotNull EditorCenter center;
    private @Nullable JBList<TestCaseDto> list;
    private @Nullable CollectionListModel<TestCaseDto> model;
    private @Nullable JBTable gridTable;
    private @NotNull RunEditorContextMenu contextMenu;
    private @Nullable JBScrollPane gridScrollPane;
    private @NotNull JBScrollPane listScrollPane;
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

    /**
     * Test case selected before a reload. Held as an id, not as a dto: a reload
     * hands back different objects for the same test cases and TestCaseDto has no
     * equals, so identity would not survive it.
     */
    private @Nullable UUID selectionToRestore;

    /**
     * Grid column selected before a reload, so the cell comes back, not just the row.
     */
    private int gridColumnToRestore = -1;

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
        toolBar = new RunToolbar(p, this);
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
        center = new EditorCenter(mainPanel);
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
                        final TestCaseDto indexed = indexer.getTestCaseById(item.getId());

                        // A case deleted since the run leaves its result behind, and
                        // the result is what the run is a record of. The row stays,
                        // says so, and takes the one status a tester cannot give.
                        //
                        // In memory only. The file heals the next time the run is
                        // written, the way the missing-stamp repair already does -
                        // opening a run rewrites nothing.
                        if (indexed == null) {
                            Logger.warn("Test run references a deleted test case id=" + item.getId());
                            item.setStatus(TestStatus.REMOVED);
                        }

                        final TestCaseDto testCase = indexed != null ? indexed : TestCaseDto.deleted(item.getId());

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

                    // Before refreshView reads currentPage: the reload may have moved
                    // the remembered test case onto a different page.
                    jumpToPageOfPendingSelection();

                    if (list != null) {
                        list.setPaintBusy(false);
                        if (allTestCases.isEmpty()) {
                            list.getEmptyText().setText("No test cases found in this run.");
                        }
                    }
                    // Also the first paint's answer: Stop starts hidden because a run
                    // that has just loaded is not executing.
                    refreshExecutionButtons();
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
    public void onToolBarSearchValueChanged() {
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

        // Only what is on screen. Re-measuring the cards costs a full pass over
        // the page, and doing it while the grid is showing buys nothing - the
        // list is re-measured when it comes back instead.
        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[details] grid active -> toggling column visibility");
            updateGridColumns();
        } else {
            refreshCards();
        }
    }

    private void refreshCards() {
        if (list != null && model != null) model.allContentsChanged();
    }

    private void updateGridColumns() {
        if (gridTable == null) return;
        gridPanelBuilder.applyColumnVisibility(gridTable, RunEditorAttributes.class, getSelectedDetails());
    }

    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + toolBar.getCurrentView());
        center.set(listScrollPane);

        // Attributes ticked while the grid was showing did not touch the cards;
        // they are re-measured here, once, rather than on every tick.
        refreshCards();
    }

    @Override
    public void onToolBarSwitchedToGridView() {
        Logger.debug("[switch] -> GRID view, currentView=" + toolBar.getCurrentView());
        rebuildGrid();
        // rebuildGrid() swallows failures; the grid parts are then still null
        // and the previous center stays visible instead of an NPE.
        if (gridScrollPane != null) center.set(gridScrollPane);
        if (gridTable != null) SwingUtilities.invokeLater(gridTable::requestFocusInWindow);
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());

        // Before anything is cleared: the timer holds the item it is counting,
        // and everything it is counted into is about to be thrown away.
        haltExecution();

        toolBar.clearFiltersAndSearch();

        rememberSelection();

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
    public @NotNull String cardTitle(final int globalIndex, final @NotNull TestCaseDto tc) {
        final Set<RunEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(globalIndex,
                selected.contains(RunEditorAttributes.ORDER),
                selected.contains(RunEditorAttributes.DESCRIPTION) ? tc.getDescription() : "");
    }

    @Override
    public @NotNull Set<RunEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(RunDetailsPopupBtn.class).getSelectedDetails();
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

        final UUID selectedId = selectionToRestore != null
                ? selectionToRestore
                : (list != null && list.getSelectedValue() != null ? list.getSelectedValue().getId() : null);

        if (model != null) {
            model.replaceAll(pageItems);
        }

        // Matched by id: a reload hands back different dto instances for the same
        // test cases, so comparing objects would drop the selection.
        if (selectedId != null && list != null) {
            for (final TestCaseDto item : pageItems) {
                if (selectedId.equals(item.getId())) {
                    // Selected by value, not by index: the list model owns its own
                    // ordering, so an index into pageItems is not safe to reuse.
                    list.setSelectedValue(item, true);
                    break;
                }
            }
        }
        selectionToRestore = null;

        statusBar.updatePaginationState(page.page(), page.totalPages(), total);
        showExecutionTotal();

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            if (gridScrollPane != null) center.set(gridScrollPane);
        }
    }

    /**
     * Records the selected test case (and grid column) before the data is reloaded.
     */
    private void rememberSelection() {
        final TestCaseDto selected = list != null ? list.getSelectedValue() : null;
        selectionToRestore = selected != null ? selected.getId() : null;
        gridColumnToRestore = gridTable != null ? gridTable.getSelectedColumn() : -1;
    }

    /**
     * Moves to whichever page now holds the remembered test case, so a selection
     * that a reload pushed onto another page is not lost.
     */
    private void jumpToPageOfPendingSelection() {
        final int page = PageWindow.pageContaining(selectionToRestore, currentTestCases, pageSize);

        // Not on any page anymore - the case was deleted or filtered out, so
        // there is nothing left to restore.
        if (page == 0) selectionToRestore = null;
        else currentPage = page;
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

                GridPanelBuilder.restoreSelection(gridTable, currentList, pageItems, gridColumnToRestore);
            }
            // Cleared regardless of whether the row was found, so a stale column can never
            // be applied to an unrelated rebuild.
            gridColumnToRestore = -1;

            gridScrollPane = new JBScrollPane(gridTable);
            Logger.debug("[grid] rebuildGrid done, rows=" + gridTable.getRowCount() + ", cols=" + gridTable.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
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
        final EditorFilters filters = EditorFilters.of(toolBar);
        // Status is the run editor's alone - a test case does not have one.
        final Set<TestStatus> statusFilter = toolBar.getToolbarItem(FilterPopupBtn.class).getSelectedStatus();

        synchronized (allTestCases) {
            return TestCaseFilter.filter(
                    allTestCases,
                    filters.query(),
                    filters.groups(),
                    filters.priorities(),
                    filters.modules(),
                    statusFilter,
                    resultsMap::get);
        }
    }

    @Override
    public void dispose() {
        // Closing the tab mid-execution is a stop the tester did not press: the
        // seconds ticked onto the in-flight case and the run's end stamp would
        // otherwise live only in this editor. Guarded because the project itself
        // may be closing, and a service asked for during that throws.
        if (isExecuting()) {
            try {
                stopExecution();
                Services.getInstance(p, RunStatusService.class).persistRun(p, this);
            } catch (final Exception ex) {
                Logger.warn("Run not persisted on editor close: " + ex.getMessage());
            }
        }

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
        TestinEditor.super.dispose();

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
            final JBList<TestCaseDto> currentList = list;
            if (currentList == null) return;

            new UpdateTestRunStatusAction(p, this, currentList).onExecutionFinished(this);
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

        if (runItem == null || runItem.isRemoved()) {
            // No run data for this case, or no test case left to run: either way
            // the execution moves on. Status changes themselves go through
            // RunStatusService, which owns advance + persist.
            startTimerForIndex(globalIndex + 1);
            return;
        }

        executionTimer.start(runItem, () -> {
            // A model event, not a repaint. The card grows a Duration line the
            // moment that value stops being blank, which makes the row taller,
            // and JList re-measures a row only when the model says that row
            // changed - a repaint draws the taller content into the cached
            // height and clips it. That is why the duration stayed hidden until
            // toggling the attribute off and on forced the re-measure.
            //
            // Only when the case is on the page being viewed: contentsChanged
            // fires with index -1 for one that is not, which invalidates the
            // layout of the whole list once a second for a row nobody can see.
            if (model != null && model.contains(currentTc)) model.contentsChanged(currentTc);
            showExecutionTotal();
        });
    }

    /**
     * The run's total is the sum of what its cases measured, not a clock of its
     * own: it counts only while a case is being timed, so a stop freezes it, a
     * resume continues it, and it is back after a reopen because the case durations
     * are. A run judged from the context menu has measured nothing and shows blank,
     * as its cases do.
     */
    private void showExecutionTotal() {
        final Duration total = resultsMap.values().stream()
                .map(TestRunItems::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        statusBar.showExecutionTime(Services.getInstance(p, Tools.class).getFormattedDuration(total));
    }

    /**
     * True while test cases are being executed: startTimerForIndex sets the
     * index for each one and stopExecution clears it.
     */
    public boolean isExecuting() {
        return currentlyExecutingIndex >= 0;
    }

    /**
     * Whether execution may start. Asked by both the toolbar button and the
     * context menu action, which used to answer it differently.
     */
    public boolean canStartExecution() {
        return !isExecuting() && !parent.getMarker().getStatus().isTerminal();
    }

    /**
     * The one place that decides which of the two execution buttons is showing.
     * <p>
     * Start when idle, Stop while a run is under way, exactly as the list and grid
     * view buttons swap. It reads {@link #isExecuting()} rather than a flag of its
     * own: the executing index is already the answer, and a second copy would be a
     * second thing to keep in step - the one that drifted would leave a Stop button
     * on a finished run.
     * <p>
     * Called after every execution-state change, so nowhere else asks.
     */
    public void refreshExecutionButtons() {
        final boolean executing = isExecuting();

        final StartExecutionBtn startBtn = toolBar.getToolbarItem(StartExecutionBtn.class);
        final StopExecutionBtn stopBtn = toolBar.getToolbarItem(StopExecutionBtn.class);

        startBtn.setVisible(!executing);
        stopBtn.setVisible(executing);
        startBtn.updateEnabledState();

        toolBar.revalidate();
        toolBar.repaint();
    }

    /**
     * Ends the execution flow, wherever the end came from - the tester's Stop, the
     * last verdict, a bulk apply, the run completing. The run itself decides
     * whether it has an end to stamp: a run nobody started has none.
     * <p>
     * The caller persists. Every path that reaches this already writes the run
     * afterwards, so the stamp and the in-flight case's duration land in the file
     * together.
     */
    public void stopExecution() {
        final TestRunDto run = tr;
        if (run != null) run.markExecutionEnded();

        haltExecution();
    }

    /**
     * Stops the execution flow without saying the run ended.
     * <p>
     * Refresh needs exactly this. It throws the loaded run and its results away
     * and reads them again, and the timer used to survive that: it kept ticking
     * an item that was no longer in the map, so that case's duration went
     * nowhere, and the reloaded run still showed the Stop button for an
     * execution nothing was driving. What it must not do is stamp the run's end
     * - refreshing is not finishing, and a run refreshed halfway would report an
     * end time the tester never asked for.
     */
    private void haltExecution() {
        executionTimer.stop();
        currentlyExecutingIndex = -1;
        refreshExecutionButtons();
    }

    @Override
    public void onStartExecutionClicked() {
        final JBList<TestCaseDto> currentList = list;
        final TestRunDto run = tr;
        if (currentList == null || run == null) return;

        // Before the status change, which is what persists the run.
        run.markExecutionStarted();
        new UpdateTestRunStatusAction(p, this, currentList).applyStatusChange(this, TestRunStatus.IN_PROGRESS);
        startTimerForIndex(firstPendingIndex());
        refreshExecutionButtons();
    }

    /**
     * Where execution starts: the first case the run has not reached yet.
     * <p>
     * Starting at zero every time re-ran the cases already given a verdict, so a
     * tester who stopped halfway, closed the editor and came back was put back at
     * the top of a run they were in the middle of.
     * <p>
     * {@code PENDING} is the right question to ask, and the only one: the run owns
     * that status and clears it only when the run itself reaches a terminal state,
     * so it survives a stop and a reopen. A case with a verdict is not pending, and
     * a case the run never reached is not either once the run has closed.
     * <p>
     * With nothing pending it falls back to the top, which is what Start has always
     * done. Returning the size instead would hand {@link #startTimerForIndex(int)}
     * its "no cases left" value and complete the run from a single click.
     */
    private int firstPendingIndex() {
        for (int i = 0; i < currentTestCases.size(); i++) {
            final TestRunItems item = resultsMap.get(currentTestCases.get(i).getId());

            if (item != null && item.getStatus() == TestStatus.PENDING) return i;
        }

        return 0;
    }

    /**
     * The tester's own stop. The confirmation lives here rather than in
     * {@link #stopExecution()} because that runs on four internal paths - the last
     * verdict, a bulk apply, the run completing - where nobody pressed anything.
     */
    @Override
    public void onStopExecutionClicked() {
        stopExecution();
        // The other stop paths persist as part of the verdict or status change they
        // belong to; this one is the tester's alone, so it writes the run itself -
        // the case duration ticked so far and the end stamp would otherwise live only
        // until the editor closed.
        Services.getInstance(p, RunStatusService.class).persistRun(p, this);
        Services.getInstance(p, Notifier.class).softShow(p, "Stopped");
    }


}
