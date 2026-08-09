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
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.grid.GridPanelBuilder;
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
import java.util.stream.Collectors;

public class TestEditor implements Disposable, IToolBar, IEditor {
    @Getter
    private final @NotNull Project p;

    @Getter
    private final TestSetDirectoryDto parent;

    private final JBPanel<?> mainPanel;

    @Getter
    private final @NotNull JBList<TestCaseDto> list;

    private final CollectionListModel<TestCaseDto> model;

    private final GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final JBScrollPane scrollPane;
    private final ModelSyncListener syncListener;
    private final Disposable projectDisposable;

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

        this.model = new CollectionListModel<>(new ArrayList<>());

        this.list = new JBList<>(model);
        list.setBackground(UIUtil.getPanelBackground());
        list.setOpaque(true);
        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading...");
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);

        FontSync.syncWithNativeEditor(p, list, projectDisposable);

        this.scrollPane = new JBScrollPane(list);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.pageSize = PropertiesComponent.getInstance().getInt("testin.pageSize", 50);

        this.toolBar = new TestToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        this.syncListener = new ModelSyncListener(this, model);
        this.syncListener.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(syncListener);

        final TestEditorContextMenu cm = new TestEditorContextMenu(p, this, parent, list, model);
        final MouseListenerImpl mouseListenerImpl = new MouseListenerImpl(p, this, list, model, parent, cm);

        list.addMouseListener(mouseListenerImpl);
        list.addMouseWheelListener(mouseListenerImpl);
        list.addMouseMotionListener(mouseListenerImpl);

        list.setTransferHandler(new TransferListener(this));
        list.setCellRenderer(new TestListRenderer(p, this));

        cm.registerShortcuts(list, cm);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        currentCenter = scrollPane;

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);
        list.addListSelectionListener(new SelectionListener(p, list, this, parent.getPath2()));

        list.addKeyListener(new KeyListener(p, list, this));

        new TestCaseExecutionSubscriber(p, list, projectDisposable);

        loadDataAsync();
    }

    private void loadDataAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
            indexer.awaitIndexing();

            final List<TestCaseDto> items = indexer.getTestCasesForTestSet(parent.getPath());

            if (items.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    list.setPaintBusy(false);
                    list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
                });
                return;
            }

            Services.getInstance(p, TestCaseCacheService.class).load(items);

            final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(p, items);

            ApplicationManager.getApplication().invokeLater(() -> {
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

        this.currentPage = (index / pageSize) + 1;
        refreshView();

        final int localIndex = index % pageSize;
        SwingUtilities.invokeLater(() -> {
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

        final VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(parent.getPath().toFile());
        if (vDir != null) vDir.refresh(false, true);

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
        new CreateTestCaseAction(p, this, parent, list);
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
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());
        FilterPopupBtn toolBarFilter = toolBar.getToolbarItem(FilterPopupBtn.class);
        if (toolBarFilter != null) {
            toolBarFilter.resetToolBarFilter();
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
        final int totalPages = getTotalPages(currentTestCases);

        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        final int startIndex = (currentPage - 1) * pageSize;
        final int endIndex = Math.min(startIndex + pageSize, totalItems);
        final List<TestCaseDto> pageItems = startIndex < totalItems
                ? new ArrayList<>(currentTestCases.subList(startIndex, endIndex))
                : new ArrayList<>();

        final TestCaseDto selectedItem = list.getSelectedValue();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        if (selectedItem != null && pageItems.contains(selectedItem)) {
            list.setSelectedValue(selectedItem, true);
        }

        statusBar.updatePaginationState(currentPage, totalPages, totalItems);

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            setCenter(gridScrollPane);
        }
    }

    private List<TestCaseDto> getCurrentPageItems() {
        final int totalItems = currentTestCases.size();
        final int startIndex = (currentPage - 1) * pageSize;
        final int endIndex = Math.min(startIndex + pageSize, totalItems);
        return startIndex < totalItems
                ? new ArrayList<>(currentTestCases.subList(startIndex, endIndex))
                : new ArrayList<>();
    }

    private void rebuildGrid() {
        final List<TestCaseDto> pageItems = getCurrentPageItems();
        final Set<TestEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            gridTable = gridPanelBuilder.buildTestTable(p, pageItems, attributes);
            FontSync.syncWithNativeEditor(p, gridTable, projectDisposable);

            gridTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                final int row = gridTable.getSelectedRow();
                if (row >= 0 && row < pageItems.size()) {
                    selectTestCase(pageItems.get(row));
                }
            });

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
        return filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / pageSize);
    }

    public void sortAndIdentifyUnsorted() {
        if (allTestCases.isEmpty()) return;

        synchronized (allTestCases) {
            final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(p, new ArrayList<>(allTestCases));

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

        if (allTestCases.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (allTestCases) {
            return allTestCases.stream()
                    .filter(tc -> {
                        final boolean matchesSearch = query.isEmpty() || tc.getDescription().toLowerCase().contains(query) || tc.getId().toString().toLowerCase().contains(query) || tc.getExpectedResult().toLowerCase().contains(query) || tc.getSteps().stream().anyMatch(step -> step != null && step.toLowerCase().contains(query));
                        final boolean matchesPriority = priorityFilter.isEmpty() || priorityFilter.contains(tc.getPriority());
                        final boolean matchesGroup = groupFilter.isEmpty() || (groupFilter.contains(Group.UNASSIGNED) && (tc.getGroup().isEmpty())) || (tc.getGroup().stream().anyMatch(groupFilter::contains));
                        final boolean matchesModule = moduleFilter.isEmpty() || moduleFilter.contains(tc.getModule());

                        return matchesSearch && matchesGroup && matchesPriority && matchesModule;
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void dispose() {
        for (MouseListener listener : list.getMouseListeners())
            list.removeMouseListener(listener);

        toolBar.dispose();

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