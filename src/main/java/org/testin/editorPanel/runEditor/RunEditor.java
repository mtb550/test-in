package org.testin.editorPanel.runEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.Dialogs.RunOpeningForm;
import org.testin.editorPanel.IEditor;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.grid.GridPanelBuilder;
import org.testin.editorPanel.listeners.*;
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
import org.testin.settings.AppSettingsState;
import org.testin.testCase.TestCaseSorter;
import org.testin.testRun.UpdateTestRunStatusAction;
import org.testin.util.FontSync;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RunEditor implements Disposable, IToolBar, IEditor {

    private final @NotNull Project p;

    @Getter
    private final TestRunDirectoryDto parent;

    @Getter
    private final List<TestCaseDto> allTestCases;

    @Getter
    private final List<TestCaseDto> currentTestCases;

    @Getter
    private final @NotNull Map<UUID, TestRunItems> resultsMap;

    private final GridPanelBuilder gridPanelBuilder = new GridPanelBuilder();
    private final Disposable projectDisposable;
    CheckboxTree checklistTree;
    @Getter
    TestRunDto tr;
    private JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());
    private JBList<TestCaseDto> list;
    private CollectionListModel<TestCaseDto> model;
    private JBTable gridTable;
    private RunEditorContextMenu contextMenu;
    private JBScrollPane gridScrollPane;
    private JBScrollPane listScrollPane;
    private JComponent currentCenter;
    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize = 50;

    @Getter
    private StatusBar statusBar;

    @Getter
    @NotNull
    private AbstractToolbarPanel toolBar;

    @Getter
    @Setter
    private String hoveredIconAction = null;

    @Getter
    @Setter
    private int hoveredIndex = -1;

    private Timer executionTimer;

    private long currentTestStartTime;

    @Getter
    private int currentlyExecutingIndex = -1;

    public RunEditor(final @NotNull Project p, final UnifiedVirtualFile vf) {
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

        FontSync.syncWithNativeEditor(p, list, projectDisposable);
    }

    private void buildOpeningPanel() {
        toolBar = new RunToolBar(p, this);
        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        RunOpeningForm openingForm = new RunOpeningForm(toolBar, statusBar);
        mainPanel = openingForm.getMainPanel();
        list = openingForm.getList();
        model = openingForm.getModel();
        listScrollPane = openingForm.getScrollPane();
        currentCenter = listScrollPane;

        list.setCellRenderer(new RunListRenderer(p, this));

        this.contextMenu = new RunEditorContextMenu(p, this, parent, list);
        final MouseListenerImpl mouseListenerImpl = new MouseListenerImpl(p, this, list, model, parent, this.contextMenu);

        list.addMouseListener(mouseListenerImpl);
        list.addMouseWheelListener(mouseListenerImpl);
        list.addMouseMotionListener(mouseListenerImpl);

        this.contextMenu.registerShortcuts(list, this.contextMenu);

        ArrayList<String> selectionPath = parent.getPath2();
        list.addListSelectionListener(new SelectionListener(p, list, this, selectionPath));

        list.setExpandableItemsEnabled(false);
        refreshView();
    }

    private void loadDataAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (this.tr == null && parent != null) {
                    final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                    indexer.awaitIndexing();

                    final Path dirPath = parent.getPath();

                    this.tr = indexer.getTestRunByPath(dirPath);
                }

                if (this.tr != null) {
                    Map<UUID, TestRunItems> newResults = this.tr.getResults().stream().collect(Collectors.toMap(TestRunItems::getId, item -> item, (existingItem, duplicateItem) -> existingItem));
                    this.resultsMap.putAll(newResults);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        StartExecutionBtn startBtn = toolBar.getToolbarItem(StartExecutionBtn.class);
                        if (startBtn != null) {
                            startBtn.updateEnabledState();
                        }
                    });
                }
            } catch (final Exception ex) {
                Logger.error("Failed to load Test Run data from disk: " + ex.getMessage());
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (this.list != null) {
                    this.list.setPaintBusy(true);
                    this.list.getEmptyText().setText("Loading...");
                }

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);
                    indexer.awaitIndexing();

                    final List<TestCaseDto> loadedItems = new ArrayList<>();

                    if (tr != null && !tr.getResults().isEmpty()) {
                        for (final TestRunItems item : tr.getResults()) {
                            final TestCaseDto tc = indexer.getTestCaseById(item.getId());
                            if (tc == null) {
                                Logger.warn("Test run references missing test case id=" + item.getId());
                                continue;
                            }
                            loadedItems.add(tc);
                            final TestRunItems runItem = resultsMap.get(item.getId());
                            if (runItem != null) {
                                runItem.setTc(tc);
                            }
                        }
                    }

                    final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(p, loadedItems).sortedList();
                    Services.getInstance(p, TestCaseCacheService.class).load(sorted);

                    ApplicationManager.getApplication().invokeLater(() -> {
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

                        refreshView();
                    });
                });
            });
        });
    }

    @Override
    public void onToolBarSearchValueChanged(final String query) {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());
        currentPage = 1;
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
        setCenter(gridScrollPane);
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        Logger.debug("[refresh] clicked, currentView=" + toolBar.getCurrentView());
        FilterPopupBtn toolBarFilter = toolBar.getToolbarItem(FilterPopupBtn.class);
        if (toolBarFilter != null)
            toolBarFilter.resetToolBarFilter();

        SearchTxt toolBarSearch = toolBar.getToolbarItem(SearchTxt.class);
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
    public Set<RunEditorAttributes> getSelectedDetails() {
        AbstractToolbarPanel baseToolBar = getToolBar();
        RunDetailsPopupBtn popup = baseToolBar.getToolbarItem(RunDetailsPopupBtn.class);
        if (popup != null) {
            return popup.getSelectedDetails();
        }
        return Collections.emptySet();
    }

    @Override
    public int getTotalPageCount() {
        return Math.max(1, (int) Math.ceil((double) currentTestCases.size() / pageSize));
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCases.size();
    }

    @Override
    public void appendNewTestCase(final TestCaseDto tc) {
        this.allTestCases.add(tc);
        refreshView();
    }

    public void refreshView() {
        final int total = currentTestCases.size();
        final int totalPages = getTotalPageCount();
        currentPage = Math.clamp(currentPage, 1, totalPages);

        final int fromIndex = (currentPage - 1) * pageSize;
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<TestCaseDto> pageItems = currentTestCases.subList(fromIndex, toIndex);

        final TestCaseDto selectedItem = list != null ? list.getSelectedValue() : null;

        if (model != null) {
            model.replaceAll(pageItems);
        }

        if (selectedItem != null && pageItems.contains(selectedItem) && list != null) {
            list.setSelectedValue(selectedItem, true);
        }

        if (statusBar != null) {
            statusBar.updatePaginationState(currentPage, totalPages, total);
        }

        if (toolBar.getCurrentView() == ViewMode.GRID_VIEW) {
            Logger.debug("[refreshView] grid active -> rebuilding grid");
            rebuildGrid();
            setCenter(gridScrollPane);
        }
    }

    private List<TestCaseDto> getCurrentPageItems() {
        final int total = currentTestCases.size();
        final int fromIndex = (currentPage - 1) * pageSize;
        final int toIndex = Math.min(fromIndex + pageSize, total);
        return currentTestCases.subList(fromIndex, toIndex);
    }

    private void rebuildGrid() {
        final List<TestCaseDto> pageItems = getCurrentPageItems();
        final Set<RunEditorAttributes> attributes = getSelectedDetails();
        Logger.debug("[grid] rebuildGrid start, pageItems=" + pageItems.size() + ", details=" + attributes);
        try {
            gridTable = gridPanelBuilder.buildRunTable(p, pageItems, attributes, resultsMap);
            FontSync.syncWithNativeEditor(p, gridTable, projectDisposable);

            gridTable.getSelectionModel().addListSelectionListener(new GridSelectionListener(this, gridTable, pageItems));
            gridTable.addMouseListener(new GridContextMenuListener(gridTable, list, contextMenu, pageItems));

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

    @Override
    public Set<String> getAvailableModules() {
        Services.getInstance(p, ProjectIndexer.class);
        final Set<String> modules = new HashSet<>();
        for (final TestCaseDto tc : allTestCases) {
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
        final Set<TestStatus> statusFilter = filterPopup != null ? filterPopup.getSelectedStatus() : Collections.emptySet();

        if (allTestCases.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (allTestCases) {
            return allTestCases.stream()
                    .filter(tc -> {
                        final boolean matchesSearch = query.isEmpty() || tc.getDescription().toLowerCase().contains(query) || tc.getId().toString().toLowerCase().contains(query) || tc.getExpectedResult().toLowerCase().contains(query) || tc.getSteps().stream().anyMatch(step -> step != null && step.toLowerCase().contains(query));
                        final boolean matchesPriority = priorityFilter.isEmpty() || priorityFilter.contains(tc.getPriority());
                        final boolean matchesGroup = groupFilter.isEmpty() || (groupFilter.contains(Group.UNASSIGNED) && tc.getGroup().isEmpty()) || (tc.getGroup().stream().anyMatch(groupFilter::contains));
                        final boolean matchesModule = moduleFilter.isEmpty() || moduleFilter.contains(tc.getModule());

                        boolean matchesStatus = statusFilter.isEmpty();
                        if (!matchesStatus) {
                            TestRunItems runItem = resultsMap.get(tc.getId());
                            if (runItem != null) {
                                matchesStatus = statusFilter.contains(runItem.getStatus());
                            }
                        }

                        return matchesSearch && matchesGroup && matchesPriority && matchesModule && matchesStatus;
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void dispose() {
        if (list != null)
            for (MouseListener listener : list.getMouseListeners())
                list.removeMouseListener(listener);

        toolBar.dispose();

        allTestCases.clear();
        resultsMap.clear();
        if (model != null) model.removeAll();
        if (mainPanel != null) mainPanel.removeAll();
        IEditor.super.dispose();

        Logger.debug("dispose run editor: " + parent.getName() + " - " + parent.getPath());

    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        if (list != null) return list;
        if (checklistTree != null) return checklistTree;
        return mainPanel;
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void updateSequenceAndSaveAll() {
    }

    @Override
    public void selectTestCase(final TestCaseDto tc) {
        if (tc == null) return;
        if (model != null) {
            final int index = model.getItems().indexOf(tc);
            if (index != -1 && list != null) {
                list.setSelectedIndex(index);
                list.ensureIndexIsVisible(index);
            }
        }
    }

    @Override
    public Set<UUID> getUnsortedIds() {
        return Collections.emptySet();
    }

    @Override
    public List<TestCaseDto> getSelectedTestCases() {
        return list != null ? list.getSelectedValuesList() : Collections.emptyList();
    }

    public void startTimerForIndex(final int globalIndex) {
        if (globalIndex >= currentTestCases.size()) {
            UpdateTestRunStatusAction changeStatus = new UpdateTestRunStatusAction(p, this, list);
            changeStatus.onExecutionFinished(p, this);
            return;
        }

        currentlyExecutingIndex = globalIndex;

        int expectedPage = (globalIndex / pageSize) + 1;
        if (currentPage != expectedPage) {
            currentPage = expectedPage;
            refreshView();
        }

        int localIndex = globalIndex - ((currentPage - 1) * pageSize);

        if (list != null) {
            list.setSelectedIndex(localIndex);
            list.ensureIndexIsVisible(localIndex);
        }

        TestCaseDto currentTc = currentTestCases.get(globalIndex);
        TestRunItems runItem = resultsMap.get(currentTc.getId());

        if (runItem == null) {
            updateStatusAndNext(TestStatus.PENDING);
            return;
        }

        runItem.setDuration(Duration.ZERO);
        currentTestStartTime = System.currentTimeMillis();

        if (executionTimer != null) executionTimer.stop();

        executionTimer = new Timer(1000, e -> {
            long seconds = (System.currentTimeMillis() - currentTestStartTime) / 1000;
            runItem.setDuration(Duration.ofSeconds(seconds));

            if (list != null) {
                list.repaint();
            }
        });
        executionTimer.start();
    }

    public void updateStatusAndNext(TestStatus status) {
        if (currentlyExecutingIndex == -1) return;

        TestCaseDto currentTc = currentTestCases.get(currentlyExecutingIndex);
        TestRunItems item = resultsMap.get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            item.setExecutedBy(Services.getInstance(p, AppSettingsState.class).testerName);
        }

        persistRunDataAsync();
        startTimerForIndex(currentlyExecutingIndex + 1);
    }

    private void persistRunDataAsync() {
        if (tr == null || parent == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path dirPath = parent.getPath();

                Services.getInstance(p, ProjectIndexer.class).putTestRun(dirPath, tr);

            } catch (final Exception ex) {
                Logger.error("Failed to persist test run data: " + ex.getMessage());
            }
        });
    }

    public void stopExecution() {
        if (executionTimer != null) {
            executionTimer.stop();
        }
        currentlyExecutingIndex = -1;
    }

    @Override
    public void onStartExecutionClicked() {
        UpdateTestRunStatusAction changeStatus = new UpdateTestRunStatusAction(p, this, list);
        changeStatus.applyStatusChange(p, this, TestRunStatus.IN_PROGRESS);
        startTimerForIndex(0);
    }
}