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
import org.testin.EscapeAction;
import org.testin.editor.*;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.grid.GridView;
import org.testin.editor.list.ListPanelBuilder;
import org.testin.editor.list.ListView;
import org.testin.editor.listeners.*;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.RunToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.*;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.*;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.TestRunDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestRunDirectoryDto;
import org.testin.notifications.Done;
import org.testin.notifications.Notifier;
import org.testin.open.OpenContextMenuAction;
import org.testin.run.RunTestCases;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.runner.TestNGExecution;
import org.testin.services.RunStatusService;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.TestCaseOrder;
import org.testin.testrun.ResultAnalysisDialog;
import org.testin.testrun.TestRunStatusChange;
import org.testin.util.Display;
import org.testin.util.FontSync;
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

    @Getter
    private final @NotNull Project p;

    @Getter
    private final @NotNull TestRunDirectoryDto parent;

    @Getter
    private final @NotNull List<TestCaseDto> allTestCases;

    @Getter
    private final @NotNull List<TestCaseDto> currentTestCases;

    /**
     * What the run recorded for each case, by case id. Not handed out: callers
     * ask {@link #runItem(UUID)}, so "this case has no run item" is one answer
     * rather than eight map lookups each checked for null (#71).
     */
    private final @NotNull Map<UUID, TestRunItems> resultsMap;

    private final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final @NotNull Disposable projectDisposable;
    private final @NotNull RunExecutionTimer executionTimer = new RunExecutionTimer();

    /**
     * Cases launched from this editor and not yet reported on.
     * <p>
     * An execution report names the case and nothing else, and the same case can
     * sit in several runs - so this is what tells this run that the tester
     * started that case here and not in the tab beside it.
     */
    private final @NotNull Set<UUID> launchedHere = ConcurrentHashMap.newKeySet();
    /**
     * Guards against a stale in-flight load overwriting a newer one (e.g. double refresh).
     */
    private final @NotNull AtomicInteger loadGeneration = new AtomicInteger();
    /**
     * The grid, from the moment the tester first switches to it - table, scroll
     * pane and the font-sync subscription that goes with them. Empty until then,
     * and after a rebuild that failed (#66, finding 18).
     */
    private @NotNull Optional<GridView> grid = Optional.empty();
    private @NotNull JBPanel<?> mainPanel;
    private @NotNull EditorCenter center;
    private final @NotNull JBList<TestCaseDto> list;
    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull JBScrollPane listScrollPane;
    private @NotNull RunEditorContextMenu contextMenu;
    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize = TestinEditor.DEFAULT_PAGE_SIZE;

    @Getter
    private @NotNull StatusBar statusBar;

    @Getter
    @NotNull
    private AbstractToolbarPanel toolBar;

    @Getter
    @Setter
    private @NotNull String hoveredIconAction = "";

    @Getter
    @Setter
    private int hoveredIndex = -1;
    /**
     * The run being edited, and empty while a reload is replacing it. Volatile:
     * loaded off the EDT and read on it.
     */
    private volatile @NotNull Optional<TestRunDto> tr = Optional.empty();

    /**
     * What the run recorded for this case, empty when it recorded nothing - a
     * case added to the test set after the run was created, or a map still being
     * refilled by a reload.
     */
    public @NotNull Optional<TestRunItems> runItem(final @NotNull UUID id) {
        return Optional.ofNullable(resultsMap.get(id));
    }

    /**
     * The run being edited, empty while a reload is replacing it.
     */
    public @NotNull Optional<TestRunDto> run() {
        return tr;
    }

    @Getter
    private int currentlyExecutingIndex = -1;

    /**
     * Test case selected before a reload. Held as an id, not as a dto: a reload
     * hands back different objects for the same test cases and TestCaseDto has no
     * equals, so identity would not survive it.
     */
    private @NotNull Optional<UUID> selectionToRestore = Optional.empty();

    /**
     * Grid column selected before a reload, so the cell comes back, not just the row.
     */
    private int gridColumnToRestore = -1;

    /**
     * Whether the cases have been read.
     * <p>
     * Not answerable from {@code tr}, which is set on the pooled thread before the
     * cases it points at have been resolved - so an editor asked to start in that
     * window would have found an empty list and started nothing.
     */
    private boolean loaded;

    /**
     * Whether something asked this editor to start as soon as it could, and has
     * not been served yet. Cleared when it is served and when a load fails, so a
     * request that could not be met does not sit armed and fire on the next
     * unrelated reload - a toolbar Refresh, or a sync catching the editor up.
     */
    private boolean startWhenLoaded;

    public RunEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        this.p = p;
        this.parent = vf.getTestRun();

        final @NotNull Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.resultsMap = new ConcurrentHashMap<>();

        // The run editor hears about executions for the first time here. The
        // test editor and the view panel only ever repainted on a report; this
        // one records what the report said, because a run is where a verdict
        // belongs.
        TestCaseExecutionSubscriber.onReported(p, projectDisposable, this::executionReported);

        // Shared list-view construction (see ListPanelBuilder, the counterpart of
        // GridPanelBuilder). Built here rather than in buildOpeningPanel so the
        // three parts of it are final: the editor never exists without a list.
        final @NotNull ListView listView = ListPanelBuilder.build(p, projectDisposable);
        this.model = listView.model();
        this.list = listView.list();
        this.listScrollPane = listView.scrollPane();

        buildOpeningPanel(listView);
        loadDataAsync();
    }

    private void buildOpeningPanel(final @NotNull ListView listView) {
        toolBar = new RunToolbar(p, this);
        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        // Run editor specifics: the run card renderer.
        list.setCellRenderer(new RunListRenderer(p, this));

        this.contextMenu = new RunEditorContextMenu(p, this, parent, list);
        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> grid.map(GridView::table),
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
        loaded = false;
        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading...");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                indexer.awaitIndexing();
                // Snapshotted into a local: reading the volatile field twice would
                // let another thread empty it between the question and the answer.
                final @NotNull TestRunDto run = tr.orElseGet(() -> indexer.getTestRunByPath(parent.getPath()));
                tr = Optional.of(run);

                resultsMap.putAll(run.getResults().stream()
                        .collect(Collectors.toMap(TestRunItems::getId, item -> item,
                                (existingItem, duplicateItem) -> existingItem)));

                final @NotNull List<TestCaseDto> loadedItems = new ArrayList<>();
                for (final TestRunItems item : run.getResults()) {
                    final @NotNull Optional<TestCaseDto> indexed = indexer.findTestCase(item.getId());

                    // A case deleted since the run leaves its result behind, and
                    // the result is what the run is a record of. The row stays,
                    // says so, and takes the one status a tester cannot give.
                    //
                    // In memory only. The file heals the next time the run is
                    // written, the way the missing-stamp repair already does -
                    // opening a run rewrites nothing.
                    if (indexed.isEmpty()) {
                        Logger.warn("Test run references a deleted test case id=" + item.getId());
                        item.setStatus(TestStatus.REMOVED);
                    }

                    final @NotNull TestCaseDto testCase = indexed.orElseGet(() -> TestCaseDto.deleted(item.getId()));

                    loadedItems.add(testCase);
                    runItem(item.getId()).ifPresent(runItem -> runItem.setTc(testCase));
                }

                final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(loadedItems);
                Services.getInstance(p, TestCaseCacheService.class).load(ordered);

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    allTestCases.clear();
                    allTestCases.addAll(ordered);
                    currentTestCases.clear();
                    currentTestCases.addAll(ordered);

                    // Before refreshView reads currentPage: the reload may have moved
                    // the remembered test case onto a different page.
                    jumpToPageOfPendingSelection();

                    list.setPaintBusy(false);
                    if (allTestCases.isEmpty()) {
                        list.getEmptyText().setText("No test cases found in this run.");
                    }
                    // Also the first paint's answer: Stop starts hidden because a run
                    // that has just loaded is not executing.
                    refreshExecutionButtons();
                    refreshView();
                    focusIfGoingTo();

                    loaded = true;
                    startIfAsked();
                });
            } catch (final Exception ex) {
                Logger.error("Failed to load Test Run data from disk: " + ex.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    list.setPaintBusy(false);
                    list.getEmptyText().setText("Unable to load this test run.");

                    // The request goes with the load that could not serve it.
                    startWhenLoaded = false;
                });
            }
        });
    }

    @Override
    public void onToolBarSearchValueChanged() {
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarSearchFocusReleased() {
        list.requestFocusInWindow();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        currentPage = 1;
        refreshView();
    }

    /**
     * Refreshes status-dependent filtering without losing the page the user is viewing.
     */
    public void refreshAfterStatusChange() {
        refreshView();
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
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
        model.allContentsChanged();
    }

    private void updateGridColumns() {
        grid.ifPresent(view ->
                gridPanelBuilder.applyColumnVisibility(view.table(), RunEditorAttributes.class, getSelectedDetails()));
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
        // rebuildGrid() swallows failures; the grid is then still empty and the
        // previous center stays visible instead of an NPE.
        grid.ifPresent(view -> {
            center.set(view.scrollPane());
            SwingUtilities.invokeLater(view.table()::requestFocusInWindow);
        });
    }

    /**
     * What the run means, in the tester's words, kept on the run itself.
     * <p>
     * The counts handed to the dialog are read from the results here rather than
     * stored with the text: they are derived, and a stored copy would be wrong
     * the moment a verdict changed.
     */
    @Override
    public void onToolBarResultAnalysisClicked() {
        run().ifPresent(runData -> new ResultAnalysisDialog(p,
                TestRunSummary.of(runData.getResults()),
                runData.getResultAnalysis(),
                analysis -> {
                    // Only the sections written in. Four empty strings were
                    // stored for the ones left alone, which every reader then
                    // treated as nothing anyway.
                    runData.setResultAnalysis(ResultAnalysis.written(analysis));
                    Services.getInstance(p, ProjectIndexer.class).putTestRun(parent.getPath(), runData);
                    Services.getInstance(p, Notifier.class).softShow(p, Done.SAVED);
                }).show());
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());

        reload();
    }


    /**
     * Re-read and redrawn. The execution stops first: the timer holds the item
     * it is counting and everything it is counted into is about to be thrown
     * away.
     */
    @Override
    public void reload() {
        haltExecution();

        toolBar.clearFiltersAndSearch();

        rememberSelection();

        this.allTestCases.clear();
        this.currentTestCases.clear();
        this.resultsMap.clear();

        this.tr = Optional.empty();

        this.model.removeAll();

        this.list.setPaintBusy(true);
        this.list.getEmptyText().setText("Refreshing...");

        loadDataAsync();
    }

    @Override
    public @NotNull String cardTitle(final @NotNull TestCaseDto tc) {
        final @NotNull Set<RunEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(positionOf(tc),
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
    public int getShownItemsCount() {
        return currentTestCases.size();
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    public void refreshView() {
        // Recomputed here rather than by the callers, as the test editor does.
        // The view is a filtered page of the master list, so anything that
        // changes that list has to show at once - and leaving it to whoever
        // changed the data means the one caller that forgets leaves a deleted
        // test case on screen. Four callers hand-copied these two lines before
        // every call, which is four chances to be the one that forgets.
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final int total = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(total, currentPage, pageSize);
        currentPage = page.page();
        // Copy: listeners retain this list, and a live subList view would throw
        // ConcurrentModificationException after currentTestCases is next mutated.
        final @NotNull List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        // What was selected before the reload, or what is selected right now.
        // Swing answers null for an empty selection, which is converted here.
        final @NotNull Optional<UUID> selectedId = selectionToRestore
                .or(() -> Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));

        model.replaceAll(pageItems);

        // Matched by id: a reload hands back different dto instances for the same
        // test cases, so comparing objects would drop the selection.
        selectedId.ifPresent(id -> {
            for (final TestCaseDto item : pageItems) {
                if (id.equals(item.getId())) {
                    // Selected by value, not by index: the list model owns its own
                    // ordering, so an index into pageItems is not safe to reuse.
                    list.setSelectedValue(item, true);
                    break;
                }
            }
        });
        selectionToRestore = Optional.empty();

        statusBar.updatePaginationState(page.page(), page.totalPages());

        // After the selection has been restored above, which is the whole point:
        // the label says what is selected, so it cannot be written before that
        // is known.
        refreshSelectionStatus(list.getSelectedIndices());
        showRunTotals();

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            grid.ifPresent(view -> center.set(view.scrollPane()));
        }
    }

    /**
     * Redraws everything a run's status changes.
     * <p>
     * The cards carry the run's status, the page indicator is rebuilt with them,
     * and Start and Stop depend on whether the run is in progress. Here rather
     * than at the caller so the list stays this editor's own - the status change
     * used to be handed the list to repaint.
     */
    @Override
    public boolean hasRunStatuses() {
        return true;
    }

    public void refreshAfterRunStatusChanged() {
        list.repaint();
        statusBar.updatePaginationState(currentPage, getTotalPageCount());
        // Not only the new status: completing a run turns every pending case into
        // untested, so the verdict counts beside it changed too and would have
        // stayed on the old numbers until the next redraw.
        showRunTotals();
        refreshExecutionButtons();
    }

    /**
     * Records the selected test case (and grid column) before the data is reloaded.
     */
    private void rememberSelection() {
        // Swing answers null when nothing is selected, which is the one thing
        // there is nothing to remember about.
        selectionToRestore = Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId);
        gridColumnToRestore = grid.map(view -> view.table().getSelectedColumn()).orElse(-1);
    }

    /**
     * Moves to whichever page now holds the remembered test case, so a selection
     * that a reload pushed onto another page is not lost.
     */
    private void jumpToPageOfPendingSelection() {
        final int page = selectionToRestore
                .map(id -> PageWindow.pageContaining(id, currentTestCases, pageSize))
                .orElse(0);

        // Not on any page anymore - the case was deleted or filtered out, so
        // there is nothing left to restore.
        if (page == 0) selectionToRestore = Optional.empty();
        else currentPage = page;
    }

    private @NotNull List<TestCaseDto> getCurrentPageItems() {
        final int total = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(total, currentPage, pageSize);
        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    private void rebuildGrid() {
        final @NotNull List<TestCaseDto> pageItems = getCurrentPageItems();
        final @NotNull Set<RunEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            final @NotNull JBTable table = gridPanelBuilder.buildRunTable(p, pageItems, attributes, resultsMap, this::positionOf);

            // The previous grid's subscription goes with the previous grid, so
            // they do not accumulate one per rebuild.
            grid.ifPresent(previous -> Disposer.dispose(previous.fontSync()));
            final @NotNull Disposable fontSync = Disposer.newDisposable(projectDisposable, "testin.runEditor.gridFontSync");
            FontSync.syncWithNativeEditor(p, table, fontSync);

            table.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, table, list, pageItems));
            // Typing into the Actual Result cell writes it to the run (#74).
            table.getModel().addTableModelListener(
                    new RunGridEditListener(p, this, pageItems, model::allContentsChanged));
            // ESC in grid view behaves like ESC in the list: hide the view panel, then clear the selection.
            new EscapeAction(p, table);
            // ENTER on the non-editable sequence column opens the details view.
            new GridViewDetailsAction(p, table, pageItems, parent.getPath2()).installDoubleClick();

            table.addMouseListener(new GridContextMenuListener(table, list, contextMenu, pageItems));
            // Every shortcut the menu offers - the verdict keys above all -
            // live on the grid too, and quiet while a cell is open (#74).
            contextMenu.bindShortcutsTo(table);
            new OpenContextMenuAction(table, contextMenu);
            GridPanelBuilder.restoreSelection(table, list, pageItems, gridColumnToRestore);

            // Cleared regardless of whether the row was found, so a stale column can never
            // be applied to an unrelated rebuild.
            gridColumnToRestore = -1;

            grid = Optional.of(new GridView(table, new JBScrollPane(table), fontSync));
            Logger.debug("[grid] rebuildGrid done, rows=" + table.getRowCount() + ", cols=" + table.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
    }


    /**
     * This editor's own node, for the toolbar's Details button. The field is
     * called parent because the test cases are its children; what the toolbar
     * wants is the node itself, so it is named for that.
     */
    @Override
    public @NotNull Project getProject() {
        return p;
    }

    @Override
    public @NotNull DirectoryDto getEditedNode() {
        return parent;
    }


    @Override
    public @NotNull Set<String> getAvailableModules() {
        final @NotNull Set<String> modules = new HashSet<>();
        for (final TestCaseDto tc : allTestCases) {
            final @NotNull String module = tc.getModule();
            if (!module.trim().isEmpty()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull EditorFilters filters = EditorFilters.of(toolBar);
        // Status is the run editor's alone - a test case does not have one.
        final @NotNull Set<TestStatus> statusFilter = toolBar.getToolbarItem(FilterPopupBtn.class).getSelectedStatus();

        synchronized (allTestCases) {
            return TestCaseFilter.filter(
                    allTestCases,
                    filters.query(),
                    filters.groups(),
                    filters.priorities(),
                    filters.modules(),
                    statusFilter,
                    this::runItem);
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
        for (final MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);

        toolBar.dispose();

        allTestCases.clear();
        resultsMap.clear();
        model.removeAll();
        mainPanel.removeAll();
        TestinEditor.super.dispose();

        Logger.debug("dispose run editor: " + parent.getName() + " - " + parent.getPath());

    }

    @Override
    public @NotNull JComponent getPreferredFocusedComponent() {
        return list;
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void updateSequenceAndSaveAll() {
    }

    @Override
    public void selectTestCase(final @NotNull TestCaseDto tc) {
        final int index = currentTestCases.indexOf(tc);
        if (index < 0) return;

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

    @Override
    public void selectWhenLoaded(final @NotNull UUID id) {
        final @NotNull Optional<TestCaseDto> loaded = currentTestCases.stream()
                .filter(tc -> id.equals(tc.getId()))
                .findFirst();

        // Already holding it, so nothing is coming to do this later: go now,
        // through the one method that owns going somewhere - it turns to the
        // right page and takes the focus with it.
        if (loaded.isPresent()) {
            selectTestCase(loaded.get());
            return;
        }

        // Not loaded yet. The load ends by moving to the page that holds the
        // remembered case and repainting, which is exactly what is wanted -
        // all that is missing is the focus, because this is a tester asking to
        // be taken somewhere rather than a reload putting things back.
        selectionToRestore = Optional.of(id);
        goingTo = true;
    }

    /**
     * Whether the pending selection is somewhere the tester asked to go, rather
     * than where they already were before a reload.
     * <p>
     * The difference is only the focus, and it matters both ways: a refresh that
     * grabbed focus would take it off whatever they were doing, and a Go To that
     * did not would leave them looking at the right row with the keyboard still
     * pointed somewhere else.
     */
    private boolean goingTo = false;

    /**
     * Focuses the list when the case that has just been restored is one the
     * tester asked to be taken to. Called once the page and the selection are
     * settled, because focusing a row that is about to be replaced is no use.
     */
    private void focusIfGoingTo() {
        if (!goingTo) return;

        goingTo = false;
        list.requestFocusInWindow();
    }

    private void selectVisibleIndex(final int index) {
        if (index < 0 || index >= list.getModel().getSize()) return;
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
        list.requestFocusInWindow();
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }

    public void startTimerForIndex(final int globalIndex) {
        if (globalIndex >= currentTestCases.size()) {
            Services.getInstance(p, TestRunStatusChange.class).apply(this, TestRunStatus.COMPLETED);
            return;
        }

        currentlyExecutingIndex = globalIndex;

        final int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        final int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        list.setSelectedIndex(localIndex);
        list.ensureIndexIsVisible(localIndex);

        final @NotNull TestCaseDto currentTc = currentTestCases.get(globalIndex);
        final @NotNull Optional<TestRunItems> runItem = runItem(currentTc.getId()).filter(item -> !item.isRemoved());

        if (runItem.isEmpty()) {
            // No run data for this case, or no test case left to run: either way
            // the execution moves on. Status changes themselves go through
            // RunStatusService, which owns advance + persist.
            startTimerForIndex(globalIndex + 1);
            return;
        }

        executionTimer.start(runItem.get(), () -> {
            // A model event, not a repaint. The card grows a Duration line
            // the moment that value stops being blank, which makes the row
            // taller. JList re-measures a row only when the model says that row
            // changed: a repaint draws the taller content into the cached height
            // and clips it. That is why the duration stayed hidden until
            // toggling the attribute off and on forced the re-measure.
            //
            // Only when the case is on the page being viewed: contentsChanged
            // fires with index -1 for one that is not, which invalidates the
            // layout of the whole list once a second for a row nobody can see.
            if (model.contains(currentTc)) model.contentsChanged(currentTc);
            showElapsed();
        });
    }

    /**
     * An execution reported on one of this run's cases.
     * <p>
     * A verdict is written into the run: the case takes its status, who ran it
     * and when. The tester clicked the run icon on a card and the run now holds
     * what happened, instead of the result living only on the badge the test
     * editor draws.
     * <p>
     * <b>The clock is not this method's.</b> Start Execution owns it, and it
     * times the case the tester is walking. Starting it again here would move it
     * onto whichever case the run icon was clicked on - and clicking one that is
     * not the current one would have taken the timer off the case actually being
     * executed and given its seconds to another.
     * <p>
     * A report about a case this run does not hold is not ours: the same case
     * can sit in several runs and be executed from any of them, and only the run
     * showing it records the result. A removed case records nothing at all.
     */
    @Override
    public void launching(final @NotNull UUID caseId) {
        launchedHere.add(caseId);

        markStartedByAutomation();
    }

    /**
     * A run with cases going has started, whoever started them.
     * <p>
     * The stamp used to belong to the Start Execution button, so a tester who
     * ran a case straight from a card got verdicts and durations written into a
     * run that still said it had never started - and every report printed a
     * blank start time for a run that plainly ran.
     * <p>
     * Once, not once per case: a selection of twelve calls this twelve times.
     * The stamp itself is idempotent, but the status change persists the marker
     * and tells the tester "In Progress", and twelve of those is eleven too
     * many. A run already in progress is left alone, and so is one signed off -
     * the same two conditions {@link #canStartExecution()} refuses on, because
     * this is the same question asked by a different gesture.
     */
    private void markStartedByAutomation() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();
        if (status == TestRunStatus.IN_PROGRESS || status.isTerminal()) return;

        run().ifPresent(TestRunDto::markExecutionStarted);
        Services.getInstance(p, TestRunStatusChange.class).apply(this, TestRunStatus.IN_PROGRESS);
    }

    /**
     * Asked from outside to run what this run has left - see
     * {@link TestinEditor#runWhenLoaded()}.
     */
    @Override
    public void runWhenLoaded() {
        startWhenLoaded = true;
        startIfAsked();
    }

    private void startIfAsked() {
        if (!startWhenLoaded || !loaded) return;

        startWhenLoaded = false;
        runPending();
    }

    /**
     * Runs the cases this run has not reached yet, and records their verdicts into
     * it.
     * <p>
     * Pending, not every case, and that is the same answer {@link #firstPendingIndex()}
     * gives the Start button beside it: re-running cases already judged puts a
     * tester back at the top of a run they were in the middle of, and it would
     * overwrite a verdict they gave by hand with one nothing asked for. A run with
     * nothing pending says so rather than quietly re-running all of it.
     * <p>
     * Cases already going are dropped here rather than left to {@link RunTestCases},
     * which drops them too - but silently, and after this has already claimed them.
     * A case claimed and then not started is a claim that never clears, so the next
     * verdict it does report, in whatever run actually ran it, is written into this
     * one as well.
     */
    private void runPending() {
        if (!canStartExecution()) {
            // The tree offers this because it cannot see inside the editor -
            // whether a run is mid-execution is known here and nowhere else. Said
            // rather than swallowed: the tab came forward and nothing happened,
            // which reads as a menu entry that does not work.
            if (isExecuting()) Services.getInstance(p, Notifier.class).softRefuseAlreadyRunning(p, parent.getName());
            return;
        }

        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        final @NotNull List<TestCaseDto> pending = allTestCases.stream()
                .filter(tc -> runItem(tc.getId()).filter(item -> item.getStatus() == TestStatus.PENDING).isPresent())
                .filter(tc -> !execution.isRunning(tc.getId()))
                .toList();

        if (pending.isEmpty()) {
            Services.getInstance(p, Notifier.class).softRefuseNothingToRun(p, parent.getName());
            return;
        }

        Logger.info("Running " + parent.getName() + " with " + pending.size() + " pending test case(s)");

        // Claimed before the launch, so every verdict that comes back lands in this
        // run rather than in whichever run editor happens to hold the same case.
        pending.forEach(tc -> launching(tc.getId()));

        RunTestCases.run(p, pending);
    }

    private void executionReported(final @NotNull TestCaseDto tc, final @NotNull RunStatus status, final @NotNull Duration duration, final @NotNull Failure failure) {
        if (!launchedHere.contains(tc.getId())) return;

        // The claim is released by anything but "started": a verdict is the end
        // of the case, and IDLE is the runner declining it - no generated
        // method, indexing in the way - or a stop putting it back. Before the
        // guards below, because a run that has since been signed off, or a case
        // since deleted, still has to let go: a claim that outlives its
        // execution hands this run the next verdict that case earns in any
        // other run.
        if (!status.stillGoing()) launchedHere.remove(tc.getId());

        if (runItem(tc.getId()).filter(item -> !item.isRemoved()).isEmpty()) return;

        // A completed or closed run keeps what it recorded. It is signed off,
        // and an execution started from somewhere else must not rewrite its
        // history - which is the same rule that stops execution being started
        // on one at all.
        if (parent.getMarker().getStatus().isTerminal()) return;

        // Empty for a report that is not a verdict - a case that has just
        // started, or one a stop put back. A stop did not find a defect, so the
        // case keeps the status it had.
        //
        // Through RunStatusService rather than by writing the fields here: it
        // already owns recording a verdict, persisting the run, ending the
        // execution flow when the verdict is for the case being walked,
        // refreshing whichever view is showing, and confirming to the tester. A
        // verdict TestNG reached is the same verdict a tester would have typed,
        // so it takes the same path.
        status.getVerdict().ifPresent(verdict ->
                Services.getInstance(p, RunStatusService.class).executeManual(p, this, tc, verdict, duration, failure));

        // A model event, not a repaint: the card grows a Duration line the
        // moment that value stops being blank, and a JList re-measures a row
        // only when the model says that row changed.
        if (model.contains(tc)) model.contentsChanged(tc);
        showRunTotals();

        // Every report changes whether anything is still running, which is what
        // decides between Start and Stop. The first case reporting RUNNING is
        // what puts Stop up; the last verdict is what takes it down again.
        refreshExecutionButtons();

        finishIfEverythingIsJudged();
    }

    /**
     * A run whose every case has a verdict is over, whoever gave them.
     * <p>
     * Completing one used to be the manual walk's business alone: the walk ran
     * off the end of the list and called it finished. So a tester who ran the
     * whole run through automation watched every card fill in and then found the
     * run still In Progress, with Start Execution offering to begin something
     * that had already happened.
     * <p>
     * Asked of the run rather than of the walk, so the answer does not depend on
     * which of the two executed it. A case deleted from the test set counts as
     * judged - the run keeps what it recorded about it and it can never be run
     * again, so waiting for it would be waiting forever.
     * <p>
     * A run already signed off is left alone: the report that reached this was
     * refused above.
     */
    private void finishIfEverythingIsJudged() {
        if (run().filter(TestRunDto::isFullyJudged).isEmpty()) return;

        Services.getInstance(p, TestRunStatusChange.class).apply(this, TestRunStatus.COMPLETED);
    }

    /**
     * Puts what the run has done so far into the status bar: how many cases carry
     * each verdict, and how long they took.
     * <p>
     * One method for the two figures because they have one set of call sites -
     * the redraw, and the two points where a verdict lands - and a second figure
     * pushed from a subset of them would be current on some screens and stale on
     * others. That is the mistake {@code refreshView} records above, where four
     * callers hand-copied the same two lines.
     * <p>
     * The counts are read from the live results rather than the run on disk, so
     * the bar moves as the tester works rather than at the next save. The
     * phrasing is not decided here: {@link ResultAnalysis#headline} owns it, so
     * the bar and the reports cannot disagree about how many passed. It is given
     * the run's status as well as its results, because one of the four buckets is
     * named for it - untouched cases are pending until the run gives up on them.
     * <p>
     * The time is the sum of what the cases measured, not a clock of its own: it
     * counts only while a case is being timed, so a stop freezes it, a resuming
     * continues it, and it is back after a reopen because the case durations are.
     * A run judged from the context menu has measured nothing and shows blank, as
     * its cases do.
     */
    private void showRunTotals() {
        final @NotNull TestRunStatus status = parent.getMarker().getStatus();

        statusBar.showRunStatus(status);
        statusBar.showVerdicts(ResultAnalysis.segments(TestRunSummary.of(List.copyOf(resultsMap.values())), status));

        showElapsed();
    }

    /**
     * The running total, and nothing else.
     * <p>
     * Separate because it alone changes every second. The timer ticks once a
     * second while a case is being timed, and it used to run the whole of
     * {@link #showRunTotals()} on each tick - summarizing the results, building
     * the verdict line and handing an html string to a label, which rebuilds its
     * view tree to accept it, all on the painting thread, for two figures that
     * cannot have changed since the tick before. A verdict is an event and is
     * pushed as one; the clock is not.
     */
    private void showElapsed() {
        final @NotNull Duration total = resultsMap.values().stream()
                .map(TestRunItems::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        statusBar.showExecutionTime(Display.formatDuration(total));
    }

    /**
     * True while test cases are being executed, by either of the two things
     * that execute them: the tester walking the run by hand, and automation
     * this editor started.
     * <p>
     * It meant only the first, so a run driven entirely by the run icon offered
     * Start throughout - a button to begin something that was already going -
     * and never offered Stop. It also left {@link #isBusy()} false, so a refresh
     * from disk was free to reload the editor while its cases were still
     * reporting into it.
     * <p>
     * Asked of the runner rather than kept as a flag here. The executing index
     * and the runner's registry are each already the answer for their half, and
     * a third copy would be the thing that drifts - which is how a Stop button
     * comes to sit on a finished run.
     */
    public boolean isExecuting() {
        return currentlyExecutingIndex >= 0 || isAutomationRunning();
    }

    /**
     * Whether anything this editor launched is still going. Only what it
     * launched: a case can be in several open runs, and another run's execution
     * is not this one's to report or to stop.
     */
    private boolean isAutomationRunning() {
        final @NotNull TestNGExecution execution = Services.getInstance(p, TestNGExecution.class);

        return launchedHere.stream().anyMatch(execution::isRunning);
    }

    /**
     * Busy while a run is being executed or a grid cell is open for editing -
     * either is live state that a reload under the tester would throw away, so an
     * on-disk refresh leaves this editor be until it is done (#20, #74).
     */
    @Override
    public boolean isBusy() {
        return isExecuting() || grid.map(GridView::isCellOpen).orElse(false);
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

        final @NotNull StartExecutionBtn startBtn = toolBar.getToolbarItem(StartExecutionBtn.class);
        final @NotNull StopExecutionBtn stopBtn = toolBar.getToolbarItem(StopExecutionBtn.class);

        startBtn.setVisible(!executing);
        stopBtn.setVisible(executing);
        startBtn.updateEnabledState();
        toolBar.getToolbarItem(ResultAnalysisBtn.class).updateEnabledState();

        toolBar.revalidate();
        toolBar.repaint();
    }

    /**
     * Ends the execution flow, wherever the end came from - the tester's Stop, the
     * last verdict, a bulk apply, the run completing. The run itself decides
     * whether it has an end to stamp: a run nobody started has none.
     * <p>
     * The caller persists. Every path that reaches this already writes the run
     * afterward, so the stamp and the in-flight case's duration land in the file
     * together.
     */
    public void stopExecution() {
        tr.ifPresent(TestRunDto::markExecutionEnded);

        haltExecution();
    }

    /**
     * Stops whatever automation this editor started, and says how much went
     * back.
     * <p>
     * The button showed while automation ran and halted only the manual walk,
     * which during an automated run is not going - so it was a Stop that did
     * nothing when pressed, the same defect the card's own stop icon had (#34).
     * <p>
     * The runner reports more cases than were asked for when they share a
     * process, which is the honest count: stopping one case of twelve in one
     * configuration stops all twelve, and every one of them is put back.
     */
    private void stopAutomation() {
        if (launchedHere.isEmpty()) return;

        final int stopped = Services.getInstance(p, TestNGExecution.class).stopCases(launchedHere);
        if (stopped == 0) return;

        Logger.info("Stopped " + stopped + " test case(s) running from '" + parent.getName() + "'");
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
        final @NotNull Optional<TestRunDto> run = run();
        if (run.isEmpty()) return;

        // Before the status change, which is what persists the run.
        run.get().markExecutionStarted();
        Services.getInstance(p, TestRunStatusChange.class).apply(this, TestRunStatus.IN_PROGRESS);
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
            if (runItem(currentTestCases.get(i).getId())
                    .filter(item -> item.getStatus() == TestStatus.PENDING)
                    .isPresent()) return i;
        }

        return 0;
    }

    /**
     * The tester's own stop, and the only one that reaches the test runner.
     * <p>
     * {@link #stopExecution()} runs on four internal paths - the last verdict, a
     * bulk apply, the run completing - where nobody pressed anything and a test
     * that is running is running legitimately. Killing it there would end a run
     * the tester never asked to end.
     */
    @Override
    public void onStopExecutionClicked() {
        // Before the walk is halted, because halting it is what stops this
        // editor claiming to be executing - and the cases to stop are read from
        // what it launched.
        stopAutomation();
        stopExecution();

        // The other stop paths persist as part of the verdict or status change they
        // belong to; this one is the tester's alone, so it writes the run itself -
        // the case duration ticked so far and the end stamp would otherwise live only
        // until the editor closed.
        Services.getInstance(p, RunStatusService.class).persistRun(p, this);
        Services.getInstance(p, Notifier.class).softShow(p, Done.STOPPED);
    }


}
