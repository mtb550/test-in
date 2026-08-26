package org.testin.editor.test;

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
import org.testin.EscapeAction;
import org.testin.editor.*;
import org.testin.editor.grid.GridPanelBuilder;
import org.testin.editor.list.ListPanelBuilder;
import org.testin.editor.grid.GridView;
import org.testin.editor.list.ListView;
import org.testin.editor.listeners.*;
import org.testin.editor.statusbar.StatusBar;
import org.testin.editor.toolbar.AbstractToolbarPanel;
import org.testin.editor.toolbar.TestToolbar;
import org.testin.editor.toolbar.Toolbar;
import org.testin.editor.toolbar.components.FilterPopupBtn;
import org.testin.editor.toolbar.components.TestDetailsPopupBtn;
import org.testin.codegen.GenType;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.TestEditorAttributes;
import org.testin.model.dto.TestCaseDto;
import org.testin.model.dto.dirs.DirectoryDto;
import org.testin.model.dto.dirs.TestSetDirectoryDto;
import org.testin.runner.TestCaseExecutionSubscriber;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testcase.CreateTestCaseAction;
import org.testin.testcase.TestCaseOrder;
import org.testin.util.FontSync;
import org.testin.view.GridViewDetailsAction;
import org.testin.view.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEditor implements Disposable, Toolbar, TestinEditor {
    @Getter
    private final @NotNull Project p;

    @Getter
    private final @NotNull TestSetDirectoryDto parent;

    private final @NotNull JBPanel<?> mainPanel;
    private final @NotNull EditorCenter center;

    @Getter
    private final @NotNull JBList<TestCaseDto> list;

    private final @NotNull CollectionListModel<TestCaseDto> model;
    private final @NotNull TestEditorContextMenu contextMenu;

    private final @NotNull GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final @NotNull JBScrollPane scrollPane;
    private final @NotNull ModelChangeNotifier modelChangeNotifier;
    private final @NotNull Disposable projectDisposable;
    /**
     * One counter for every model-replacing operation - data loads and badge
     * sorts alike (#24). Each one bumps and checks it, so a stale in-flight
     * result never overwrites a newer one, whichever kind it is.
     */
    private final @NotNull AtomicInteger modelGeneration = new AtomicInteger();
    @Getter
    @NotNull
    private final AbstractToolbarPanel toolBar;
    @Getter
    private final @NotNull StatusBar statusBar;
    @Getter
    private final @NotNull List<TestCaseDto> allTestCases;
    @Getter
    private final @NotNull List<TestCaseDto> currentTestCases;
    /**
     * The grid, from the moment the tester first switches to it - table, scroll
     * pane and the font-sync subscription that goes with them. Empty until then,
     * and after a rebuild that failed (#66, finding 18).
     */
    private @NotNull Optional<GridView> grid = Optional.empty();

    @Getter
    @Setter
    private int currentPage = 1;
    /**
     * Test case selected before a reload. Held as an id, not as a dto: a refresh
     * replaces the objects, so identity would not survive it.
     */
    private @NotNull Optional<UUID> selectionToRestore = Optional.empty();
    /**
     * Grid column selected before a reload, so the cell comes back, not just the row.
     */
    private int gridColumnToRestore = -1;

    @Getter
    @Setter
    private int pageSize;

    @Getter
    @Setter
    private @NotNull String hoveredIconAction = "";

    @Getter
    @Setter
    private int hoveredIndex = -1;
    /**
     * True from the moment a load starts until its data is on screen. The empty
     * message asks it, so a list that is empty because it is still loading keeps
     * the loading message instead of being told there is nothing to show.
     */
    private volatile boolean loading;

    public TestEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        this.p = p;
        this.parent = vf.getTestSet();

        final @NotNull Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());


        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.center = new EditorCenter(this.mainPanel);
        this.mainPanel.setBackground(UIUtil.getPanelBackground());
        this.mainPanel.setOpaque(true);

        // Shared list-view construction (see ListPanelBuilder, the counterpart of GridPanelBuilder).
        final @NotNull ListView listView = ListPanelBuilder.build(p, projectDisposable);
        this.model = listView.model();
        this.list = listView.list();
        this.scrollPane = listView.scrollPane();

        // Test editor specifics: manual reordering by drag-and-drop and the card renderer.
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferListener(p, this));
        list.setCellRenderer(new TestListRenderer(p, this));

        this.pageSize = PropertiesComponent.getInstance().getInt("testin.pageSize", 50);

        this.toolBar = new TestToolbar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);
        toolBar.installSearchFocusShortcut(mainPanel);

        this.modelChangeNotifier = new ModelChangeNotifier();
        this.modelChangeNotifier.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(modelChangeNotifier);

        this.contextMenu = new TestEditorContextMenu(p, this, parent, list, model);
        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> grid.map(GridView::table),
                () -> toolBar.getCurrentView() == ViewMode.GRID_VIEW);

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);

        new TestCaseExecutionSubscriber(p, projectDisposable, (tc, status, duration, failure) -> list.repaint());

        // List view is the default mode when the editor opens.
        onToolBarSwitchedToListView();

        loadDataAsync();
    }

    private void loadDataAsync() {
        final int generation = modelGeneration.incrementAndGet();
        loading = true;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            indexer.awaitIndexing();

            final @NotNull List<TestCaseDto> items = indexer.getTestCasesForTestSet(parent.getPath());

            if (items.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != modelGeneration.get()) return;
                    allTestCases.clear();
                    currentTestCases.clear();
                    list.setPaintBusy(false);
                    loading = false;
                    // The message comes from refreshView, which is the one place
                    // that knows what the page ended up holding.
                    refreshView();
                });
                return;
            }

            Services.getInstance(p, TestCaseCacheService.class).load(items);

            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(items);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != modelGeneration.get()) return;
                allTestCases.clear();
                allTestCases.addAll(ordered);
                currentTestCases.clear();
                currentTestCases.addAll(ordered);

                ordered.forEach(tc -> tc.setParent(parent));

                // The item may now sit on a different page than before the reload.
                jumpToPageOfPendingSelection();

                list.setPaintBusy(false);
                loading = false;

                refreshView();
                focusIfGoingTo();
            });
        });
    }

    private void onDataSynced() {
        orderThen(this::refreshView);
    }

    @Override
    public void updateSequenceAndSaveAll() {
        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCases) {
            snapshot = new ArrayList<>(this.allTestCases);
        }

        final @NotNull Path dirPath = parent.getPath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // Ranked along the order on screen, which is the order the tester
            // just arranged. A case already sitting in the right place keeps the
            // rank it had, so a drag writes the case that moved and leaves the
            // rest of the set alone.
            final @NotNull List<TestCaseDto> moved = TestCaseOrder.place(snapshot);

            Services.getInstance(p, ProjectIndexer.class).updateSequence(dirPath, snapshot, moved);

            // The generated methods carry the position, so a reorder has to
            // rewrite them or the run keeps executing in the order before the
            // drag. Every case in the set, not only the ones whose rank changed:
            // moving one case past three others changes where all four sit, and
            // a position is a number with no room between two of them.
            //
            // Skipped where there is nothing to write - an IDE with no Java
            // plugin answers with a no-op, and a set nobody has generated code
            // for has no methods to update.
            if (!snapshot.isEmpty()) GenType.UPDATE_TEST_CASE_ORDER.executeAll(p, snapshot);

            ApplicationManager.getApplication().invokeLater(this::refreshView);
        });
    }

    @Override
    public void selectTestCase(final @NotNull TestCaseDto tc) {
        if (!currentTestCases.contains(tc)) {
            toolBar.getToolbarItem(FilterPopupBtn.class).resetToolBarFilter();

            currentTestCases.clear();
            currentTestCases.addAll(getFilteredList());
        }

        final int index = currentTestCases.indexOf(tc);
        if (index == -1) return;

        final int safePageSize = Math.max(1, pageSize);
        final int page = (index / safePageSize) + 1;
        final int localIndex = index % safePageSize;

        if (page == this.currentPage) {
            list.setSelectedIndex(localIndex);
            list.ensureIndexIsVisible(localIndex);
            list.requestFocusInWindow();
            return;
        }

        this.currentPage = page;
        refreshView();

        ApplicationManager.getApplication().invokeLater(() -> {
            list.setSelectedIndex(localIndex);
            list.ensureIndexIsVisible(localIndex);
            list.requestFocusInWindow();
        });
    }

    @Override
    public void appendNewTestCase(final @NotNull TestCaseDto tc) {
        this.allTestCases.add(tc);
        orderThen(() -> {
            updateSequenceAndSaveAll();

            // VFS refresh goes through the indexer - file access is the
            // indexer's alone (see CLAUDE.md).
            Services.getInstance(p, ProjectIndexer.class).refreshDirectory(parent.getPath());

            refreshView();
            selectTestCase(tc);
        });
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(currentTestCases);
    }

    /**
     * Reported as not reachable from the entry points, and kept: this and
     * {@link #getPreferredFocusedComponent()} are the {@code FileEditor}
     * contract. The platform calls them when it shows the tab; no code in the
     * plugin does, and no inspection can see that call (#61).
     */
    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public void onToolBarCreateTestCaseClicked() {
        new CreateTestCaseAction(p, this, parent, list).openCreateDialog();
    }

    @Override
    public void onToolBarSearchValueChanged() {
        this.currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarSearchFocusReleased() {
        list.requestFocusInWindow();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        this.currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        this.currentPage = 1;
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
            model.allContentsChanged();
        }
    }

    private void updateGridColumns() {
        grid.ifPresent(view ->
                gridPanelBuilder.applyColumnVisibility(view.table(), TestEditorAttributes.class, getSelectedDetails()));
    }

    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + toolBar.getCurrentView());
        center.set(scrollPane);

        // Attributes ticked while the grid was showing did not touch the cards;
        // they are re-measured here, once, rather than on every tick.
        model.allContentsChanged();
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

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());
        reload();
    }

    @Override
    public void reload() {
        toolBar.clearFiltersAndSearch();

        rememberSelection();

        this.allTestCases.clear();
        this.currentTestCases.clear();
        this.model.removeAll();
        this.list.setPaintBusy(true);
        this.list.getEmptyText().setText("Refreshing...");

        loadDataAsync();
    }

    /**
     * Busy while a grid cell is open for editing - a reload would discard the
     * half-typed value, so an on-disk refresh leaves this editor be until the
     * tester is done (#20). The run editor answers this with its execution state
     * as well; a test set has no execution, only its cells.
     */
    @Override
    public boolean isBusy() {
        return grid.map(GridView::isCellOpen).orElse(false);
    }

    @Override
    public @NotNull String cardTitle(final int globalIndex, final @NotNull TestCaseDto tc) {
        final @NotNull Set<TestEditorAttributes> selected = getSelectedDetails();

        return BaseCard.titleText(globalIndex,
                selected.contains(TestEditorAttributes.ORDER),
                selected.contains(TestEditorAttributes.DESCRIPTION) ? tc.getDescription() : "");
    }

    @Override
    public @NotNull Set<TestEditorAttributes> getSelectedDetails() {
        return getToolBar().getToolbarItem(TestDetailsPopupBtn.class).getSelectedDetails();
    }

    public void refreshView() {
        // Recomputed here rather than by the callers: the view is a filtered page
        // of the master list, so anything that changes that list - deleting,
        // pasting, reordering - has to show at once. Leaving it to whoever changed
        // the data means the one caller that forgets leaves a deleted test case
        // on screen until the next explicit refresh.
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final int totalItems = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        currentPage = page.page();
        final @NotNull List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        // What was selected before the reload, or what is selected right now.
        // Swing answers null for an empty selection, which is converted here.
        final @NotNull Optional<UUID> selectedId = selectionToRestore
                .or(() -> Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId));

        modelChangeNotifier.pause();
        model.replaceAll(pageItems);
        modelChangeNotifier.resume();

        // Matched by id: a reload hands back different dto instances for the
        // same test cases, so comparing objects would drop the selection.
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

        showEmptyStateIfNothingToDraw(totalItems);

        statusBar.updatePaginationState(page.page(), page.totalPages(), totalItems);

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            grid.ifPresent(view -> center.set(view.scrollPane()));
        }
    }

    /**
     * What an empty list says, decided here because this is where the page is
     * decided.
     * <p>
     * It used to be set only by the two places that load data, so a list emptied
     * any other way kept whatever message was last written - remove the last
     * test case after a refresh and the editor sat on "Refreshing..." forever,
     * for a refresh that had finished minutes ago.
     * <p>
     * Two empties, two answers: nothing in the test set at all, which is an
     * invitation to add one, and nothing matching the search, which is not - the
     * cases are there and the filter is hiding them.
     * <p>
     * Silent while loading. The load paths own that message, and overwriting it
     * here would flash "No test cases found" over data that is still on its way.
     */
    private void showEmptyStateIfNothingToDraw(final int totalItems) {
        if (totalItems > 0 || loading) return;

        if (allTestCases.isEmpty()) {
            list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
        } else {
            list.getEmptyText().setText("No test cases match the search");
        }
    }

    /**
     * Records the selected test case (and grid column) before the data is reloaded.
     */
    private void rememberSelection() {
        selectionToRestore = Optional.ofNullable(list.getSelectedValue()).map(TestCaseDto::getId);
        gridColumnToRestore = grid.map(view -> view.table().getSelectedColumn()).orElse(-1);
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

    /**
     * Moves to whichever page now holds the remembered test case.
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
        final int totalItems = currentTestCases.size();
        final @NotNull PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    private void rebuildGrid() {
        final @NotNull List<TestCaseDto> pageItems = getCurrentPageItems();
        final @NotNull Set<TestEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            final @NotNull JBTable table = gridPanelBuilder.buildTestTable(p, pageItems, attributes, (currentPage - 1) * pageSize);

            // The previous grid's subscription goes with the previous grid, so
            // they do not accumulate one per rebuild.
            grid.ifPresent(previous -> Disposer.dispose(previous.fontSync()));
            final @NotNull Disposable fontSync = Disposer.newDisposable(projectDisposable, "testin.testEditor.gridFontSync");
            FontSync.syncWithNativeEditor(p, table, fontSync);

            table.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, table, list, pageItems));
            table.getModel().addTableModelListener(new GridEditListener(p, pageItems, model::allContentsChanged, parent.getPath()));
            // ESC in grid view behaves like ESC in the list: hide the view panel, then clear the selection.
            new EscapeAction(p, table);
            // ENTER on the non-editable sequence column opens the details view.
            new GridViewDetailsAction(p, table, pageItems, parent.getPath2()).installDoubleClick();
            table.addMouseListener(new GridContextMenuListener(table, list, contextMenu, pageItems));
            // Every shortcut the menu offers, live on the grid too (#74).
            contextMenu.bindShortcutsTo(table);

            GridPanelBuilder.restoreSelection(table, list, pageItems, gridColumnToRestore);
            // Cleared regardless of whether the row was found, so a stale column can
            // never be applied to an unrelated rebuild.
            gridColumnToRestore = -1;

            grid = Optional.of(new GridView(table, new JBScrollPane(table), fontSync));
            Logger.debug("[grid] rebuildGrid done, rows=" + table.getRowCount() + ", cols=" + table.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
    }


    private int getTotalPages(final @NotNull List<TestCaseDto> filtered) {
        return PageWindow.of(filtered.size(), currentPage, pageSize).totalPages();
    }

    /**
     * Re-sorts asynchronously, then persists the resulting sequence through
     * the indexer. Persisting must wait for the sort to land - callers use
     * this instead of running the two steps sequentially themselves.
     */
    public void reorderAndPersist() {
        orderThen(this::updateSequenceAndSaveAll);
    }

    /**
     * Recomputes the order and the unsorted-badge ids off the EDT (#24): the
     * walk runs on a pooled thread and the result is applied back on the EDT,
     * where onDone continues (persisting, refreshing). Any newer sort or load
     * bumps the generation, so a stale result never overwrites a newer one.
     */
    private void orderThen(final @NotNull Runnable onDone) {
        final List<TestCaseDto> snapshot;
        synchronized (allTestCases) {
            snapshot = new ArrayList<>(allTestCases);
        }
        if (snapshot.isEmpty()) {
            onDone.run();
            return;
        }

        final int generation = modelGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (generation != modelGeneration.get()) return;
            final @NotNull List<TestCaseDto> ordered = TestCaseOrder.ordered(snapshot);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != modelGeneration.get()) return;

                synchronized (allTestCases) {
                    this.allTestCases.clear();
                    this.allTestCases.addAll(ordered);
                }

                onDone.run();
            });
        });
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
        final @NotNull ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final @NotNull Set<String> modules = new HashSet<>();
        for (final TestCaseDto tc : indexer.getTestCasesForTestSet(parent.getPath())) {
            final @NotNull String module = tc.getModule();
            if (!module.trim().isEmpty()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private @NotNull List<TestCaseDto> getFilteredList() {
        final @NotNull EditorFilters filters = EditorFilters.of(toolBar);

        synchronized (allTestCases) {
            return TestCaseFilter.filter(
                    allTestCases,
                    filters.query(),
                    filters.groups(),
                    filters.priorities(),
                    filters.modules());
        }
    }

    @Override
    public void dispose() {
        // Releases the message-bus subscriptions (font sync, execution subscriber)
        // registered against this editor's lifetime.
        Disposer.dispose(projectDisposable);

        for (final MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);

        toolBar.dispose();
        statusBar.dispose();

        // Swing answers null when the editor is closing with nothing selected,
        // and then there is no case whose view panel needs closing.
        Optional.ofNullable(list.getSelectedValue()).ifPresent(selectedInThisFile ->
                ViewToolWindowFactory.panel().ifPresent(viewer -> viewer.hide(selectedInThisFile)));

        allTestCases.clear();
        currentTestCases.clear();

        model.removeListDataListener(modelChangeNotifier);
        model.removeAll();

        mainPanel.removeAll();

        TestinEditor.super.dispose();

        Logger.debug("dispose test editor: " + parent.getName() + " - " + parent.getPath());
    }

    @Override
    public @NotNull List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }
}
