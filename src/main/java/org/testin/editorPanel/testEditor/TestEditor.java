package org.testin.editorPanel.testEditor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.PageWindow;
import org.testin.editorPanel.TestCaseFilter;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.grid.GridPanelBuilder;
import org.testin.editorPanel.list.ListPanelBuilder;
import org.testin.editorPanel.list.ListView;
import org.testin.editorPanel.listeners.*;
import org.testin.editorPanel.statusBar.StatusBar;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.editorPanel.toolBar.TestToolBar;
import org.testin.editorPanel.toolBar.components.FilterPopupBtn;
import org.testin.editorPanel.toolBar.components.SearchTxt;
import org.testin.editorPanel.toolBar.components.TestDetailsPopupBtn;
import org.testin.enums.Group;
import org.testin.enums.Priority;
import org.testin.enums.TestEditorAttributes;
import org.testin.enums.ViewMode;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.mappers.dto.TestCaseDto;
import org.testin.mappers.dto.dirs.TestSetDirectoryDto;
import org.testin.services.Services;
import org.testin.services.TestCaseCacheService;
import org.testin.testCase.CreateTestCaseAction;
import org.testin.testCase.SortResult;
import org.testin.testCase.TestCaseSorter;
import org.testin.util.FontSync;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEditor implements Disposable, IToolBar, IEditor {
    @Getter
    private final @NotNull Project p;

    @Getter
    private final TestSetDirectoryDto parent;

    private final JBPanel<?> mainPanel;

    @Getter
    private final @NotNull JBList<TestCaseDto> list;

    private final CollectionListModel<TestCaseDto> model;
    private final TestEditorContextMenu contextMenu;

    private final GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final JBScrollPane scrollPane;
    private final ModelSyncListener syncListener;
    private final Disposable projectDisposable;

    /**
     * Child disposable for the current grid table's font-sync subscription;
     * replaced on every grid rebuild so old subscriptions do not accumulate.
     */
    private Disposable gridFontSyncDisposable;

    /**
     * Guards against a stale in-flight load overwriting a newer one (e.g. double refresh).
     */
    private final AtomicInteger loadGeneration = new AtomicInteger();

    @Getter
    @NotNull
    private final AbstractToolbarPanel toolBar;

    @Getter
    private final StatusBar statusBar;

    @Getter
    private final List<TestCaseDto> allTestCases;

    @Getter
    private final Set<UUID> unsortedIds;

    @Getter
    private final List<TestCaseDto> currentTestCases;

    private JBTable gridTable;

    private JBScrollPane gridScrollPane;

    private JComponent currentCenter;

    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize;

    @Getter
    @Setter
    private String hoveredIconAction = null;

    @Getter
    @Setter
    private int hoveredIndex = -1;

    public TestEditor(final @NotNull Project p, final @NotNull UnifiedVirtualFile vf) {
        this.p = p;
        this.parent = vf.getTestSet();

        final Disposable projectDisposable = Disposer.newDisposable();
        Disposer.register(p, projectDisposable);
        this.projectDisposable = projectDisposable;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.unsortedIds = Collections.synchronizedSet(new HashSet<>());

        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.mainPanel.setBackground(UIUtil.getPanelBackground());
        this.mainPanel.setOpaque(true);

        // Shared list-view construction (see ListPanelBuilder, the counterpart of GridPanelBuilder).
        final ListView listView = ListPanelBuilder.build(p, projectDisposable);
        this.model = listView.model();
        this.list = listView.list();
        this.scrollPane = listView.scrollPane();

        // Test editor specifics: manual reordering by drag-and-drop and the card renderer.
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new TransferListener(this));
        list.setCellRenderer(new TestListRenderer(p, this));
        list.addKeyListener(new KeyListener(p, list, this));

        this.pageSize = PropertiesComponent.getInstance().getInt("testin.pageSize", 50);

        this.toolBar = new TestToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        this.syncListener = new ModelSyncListener(this, model);
        this.syncListener.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(syncListener);

        this.contextMenu = new TestEditorContextMenu(p, this, parent, list, model);
        ListPanelBuilder.wireCommonListeners(p, this, listView, parent, contextMenu,
                () -> gridTable,
                () -> toolBar.getCurrentView() == ViewMode.GRID_VIEW);

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);

        new TestCaseExecutionSubscriber(p, list, projectDisposable);

        // List view is the default mode when the editor opens.
        onToolBarSwitchedToListView();

        loadDataAsync();
    }

    private void loadDataAsync() {
        final int generation = loadGeneration.incrementAndGet();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            indexer.awaitIndexing();

            final List<TestCaseDto> items = indexer.getTestCasesForTestSet(parent.getPath());

            if (items.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (generation != loadGeneration.get()) return;
                    allTestCases.clear();
                    currentTestCases.clear();
                    unsortedIds.clear();
                    list.setPaintBusy(false);
                    list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
                    refreshView();
                });
                return;
            }

            Services.getInstance(p, TestCaseCacheService.class).load(items);

            final SortResult result = TestCaseSorter.sortTestCases(p, items);

            ApplicationManager.getApplication().invokeLater(() -> {
                if (generation != loadGeneration.get()) return;
                allTestCases.clear();
                allTestCases.addAll(result.sortedList());
                currentTestCases.clear();
                currentTestCases.addAll(result.sortedList());

                unsortedIds.clear();
                unsortedIds.addAll(result.unsortedIds());

                result.sortedList().forEach(tc -> tc.setParent(parent));

                list.setPaintBusy(false);
                if (allTestCases.isEmpty()) {
                    list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
                }

                refreshView();
            });
        });
    }

    private void onDataSynced() {
        sortAndIdentifyUnsorted();
        refreshView();
    }

    @Override
    public void updateSequenceAndSaveAll() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCases) {
            snapshot = new ArrayList<>(this.allTestCases);
        }

        this.unsortedIds.clear();
        final Path dirPath = parent.getPath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            for (int i = 0; i < snapshot.size(); i++) {
                TestCaseDto current = snapshot.get(i);
                current.setIsHead(i == 0);
                current.setNext(i < snapshot.size() - 1 ? snapshot.get(i + 1).getId() : null);
            }

            Services.getInstance(p, ProjectIndexer.class).updateSequence(dirPath, snapshot);

            ApplicationManager.getApplication().invokeLater(this::refreshView);
        });
    }

    @Override
    public void selectTestCase(final TestCaseDto tc) {
        if (tc == null) return;

        if (!currentTestCases.contains(tc)) {
            FilterPopupBtn popup = toolBar.getToolbarItem(FilterPopupBtn.class);
            if (popup != null) popup.resetToolBarFilter();

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
    public void appendNewTestCase(final TestCaseDto tc) {
        this.allTestCases.add(tc);
        sortAndIdentifyUnsorted();
        updateSequenceAndSaveAll();

        // Asynchronous refresh: a synchronous recursive VFS refresh from the EDT is a platform violation.
        final VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(parent.getPath().toFile());
        if (vDir != null) vDir.refresh(true, true);

        refreshView();
        selectTestCase(tc);
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(currentTestCases);
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public void onToolBarCreateTestCaseClicked() {
        new CreateTestCaseAction(p, this, parent, list).openCreateDialog();
    }

    @Override
    public void onToolBarSearchValueChanged(final String query) {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        this.currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        this.currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        this.currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarDetailsSelectionChanged() {
        Logger.debug("[details] selectedDetails changed -> " + getSelectedDetails());
        if (model != null) {
            model.allContentsChanged();
        }
        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[details] grid active -> toggling column visibility");
            updateGridColumns();
        }
    }

    private void updateGridColumns() {
        if (gridTable == null) return;
        gridPanelBuilder.applyColumnVisibility(gridTable, Arrays.stream(TestEditorAttributes.values()).toList(), TestEditorAttributes::getName, getSelectedDetails());
    }

    @Override
    public void onToolBarSwitchedToListView() {
        Logger.debug("[switch] -> LIST view, currentView=" + toolBar.getCurrentView());
        setCenter(scrollPane);
    }

    @Override
    public void onToolBarSwitchedToGridView() {
        Logger.debug("[switch] -> GRID view, currentView=" + toolBar.getCurrentView());
        rebuildGrid();
        setCenter(gridScrollPane);
        SwingUtilities.invokeLater(gridTable::requestFocusInWindow);
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());
        FilterPopupBtn toolBarFilter = toolBar.getToolbarItem(FilterPopupBtn.class);
        if (toolBarFilter != null) {
            toolBarFilter.clearFilters();
        }

        SearchTxt toolBarSearch = toolBar.getToolbarItem(SearchTxt.class);
        if (toolBarSearch != null) {
            toolBarSearch.resetSearchQuery();
        }

        this.allTestCases.clear();
        this.currentTestCases.clear();
        this.unsortedIds.clear();
        this.model.removeAll();
        this.list.setPaintBusy(true);
        this.list.getEmptyText().setText("Refreshing...");

        loadDataAsync();
    }

    @Override
    public Set<TestEditorAttributes> getSelectedDetails() {
        AbstractToolbarPanel baseToolBar = getToolBar();
        TestDetailsPopupBtn popup = baseToolBar.getToolbarItem(TestDetailsPopupBtn.class);
        if (popup != null) {
            return popup.getSelectedDetails();
        }
        return Collections.emptySet();
    }

    public void refreshView() {
        final int totalItems = currentTestCases.size();
        final PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        currentPage = page.page();
        final List<TestCaseDto> pageItems = new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));

        final TestCaseDto selectedItem = list.getSelectedValue();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        if (selectedItem != null && pageItems.contains(selectedItem)) {
            list.setSelectedValue(selectedItem, true);
        }

        statusBar.updatePaginationState(page.page(), page.totalPages(), totalItems);

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            setCenter(gridScrollPane);
        }
    }

    private List<TestCaseDto> getCurrentPageItems() {
        final int totalItems = currentTestCases.size();
        final PageWindow page = PageWindow.of(totalItems, currentPage, pageSize);
        return new ArrayList<>(currentTestCases.subList(page.fromIndex(), page.toIndex()));
    }

    private void rebuildGrid() {
        final List<TestCaseDto> pageItems = getCurrentPageItems();
        final Set<TestEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            gridTable = gridPanelBuilder.buildTestTable(p, pageItems, attributes, (currentPage - 1) * pageSize);

            if (gridFontSyncDisposable != null) Disposer.dispose(gridFontSyncDisposable);
            gridFontSyncDisposable = Disposer.newDisposable(projectDisposable, "testin.testEditor.gridFontSync");
            FontSync.syncWithNativeEditor(p, gridTable, gridFontSyncDisposable);

            gridTable.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, gridTable, pageItems));
            gridTable.getModel().addTableModelListener(new GridEditListener(p, pageItems, model::allContentsChanged));
            gridTable.addMouseListener(new GridContextMenuListener(gridTable, list, contextMenu, pageItems));

            final TestCaseDto selectedItem = list.getSelectedValue();
            final int selectedRow = pageItems.indexOf(selectedItem);
            if (selectedRow >= 0) {
                gridTable.changeSelection(selectedRow, 0, false, false);
            }

            gridScrollPane = new JBScrollPane(gridTable);
            Logger.debug("[grid] rebuildGrid done, rows=" + gridTable.getRowCount() + ", cols=" + gridTable.getColumnCount());
        } catch (final Exception ex) {
            Logger.error("[grid] rebuildGrid FAILED: " + ex);
        }
    }

    private void setCenter(final JComponent component) {
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

    private int getTotalPages(final List<TestCaseDto> filtered) {
        return PageWindow.of(filtered.size(), currentPage, pageSize).totalPages();
    }

    public void sortAndIdentifyUnsorted() {
        if (allTestCases.isEmpty()) return;

        synchronized (allTestCases) {
            final SortResult result = TestCaseSorter.sortTestCases(p, new ArrayList<>(allTestCases));

            this.allTestCases.clear();
            this.allTestCases.addAll(result.sortedList());

            this.unsortedIds.clear();
            this.unsortedIds.addAll(result.unsortedIds());
        }
    }

    @Override
    public Set<String> getAvailableModules() {
        final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
        final Set<String> modules = new HashSet<>();
        for (final TestCaseDto tc : indexer.getTestCasesForTestSet(parent.getPath())) {
            final String module = tc.getModule();
            if (!module.trim().isEmpty()) {
                modules.add(module.trim());
            }
        }
        return modules;
    }

    private List<TestCaseDto> getFilteredList() {
        final String query = toolBar.getSearchTxt() != null
                ? toolBar.getSearchTxt().getSearchQuery() : "";

        FilterPopupBtn filterPopup;
        filterPopup = toolBar.getToolbarItem(FilterPopupBtn.class);

        final Set<Group> groupFilter = filterPopup != null ? filterPopup.getSelectedGroup() : Collections.emptySet();
        final Set<Priority> priorityFilter = filterPopup != null ? filterPopup.getSelectedPriority() : Collections.emptySet();
        final Set<String> moduleFilter = filterPopup != null ? filterPopup.getSelectedModule() : Collections.emptySet();

        synchronized (allTestCases) {
            return TestCaseFilter.filter(
                    allTestCases,
                    query,
                    groupFilter,
                    priorityFilter,
                    moduleFilter);
        }
    }

    @Override
    public void dispose() {
        // Releases the message-bus subscriptions (font sync, execution subscriber)
        // registered against this editor's lifetime.
        Disposer.dispose(projectDisposable);

        for (MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);

        toolBar.dispose();
        statusBar.dispose();

        TestCaseDto selectedInThisFile = list.getSelectedValue();

        final ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
        if (viewer != null)
            viewer.hide(selectedInThisFile);

        allTestCases.clear();
        currentTestCases.clear();
        unsortedIds.clear();

        if (model != null) {
            model.removeListDataListener(syncListener);
            model.removeAll();
        }

        if (mainPanel != null)
            mainPanel.removeAll();

        IEditor.super.dispose();

        Logger.debug("dispose test editor: " + parent.getName() + " - " + parent.getPath());
    }

    @Override
    public List<TestCaseDto> getSelectedTestCases() {
        return list.getSelectedValuesList();
    }
}
