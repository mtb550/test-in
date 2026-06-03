package org.testin.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.editorPanel.EditorCM;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.StatusBar;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.listeners.*;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.editorPanel.toolBar.RunToolBar;
import org.testin.editorPanel.toolBar.components.FilterPopup;
import org.testin.editorPanel.toolBar.components.RunDetailsPopup;
import org.testin.editorPanel.toolBar.components.SearchTxt;
import org.testin.pojo.*;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.pojo.dto.TestRunDto;
import org.testin.ui.RunOpeningForm;
import org.testin.util.FontSyncUtil;
import org.testin.util.Mapper;
import org.testin.util.TestCaseSorter;
import org.testin.util.logger.Log;
import org.testin.util.services.TestCaseCacheService;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RunEditorUI implements Disposable, IToolBar, IEditorUI {

    @Getter
    private final Project project;
    @Getter
    private final UnifiedVirtualFile vf;

    @Getter
    private final List<TestCaseDto> allTestCases;

    @Getter
    private final List<TestCaseDto> currentTestCases;

    @Getter
    private final @NotNull Map<UUID, TestRunItems> resultsMap;

    CheckboxTree checklistTree;

    TestRunDto tr;

    private RunSessionCache sessionCache;

    private JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

    private JBList<TestCaseDto> list;

    private CollectionListModel<TestCaseDto> model;

    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize = 50;

    @Getter
    private StatusBar statusBar;

    @Getter
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

    public RunEditorUI(final @NotNull Project project, final UnifiedVirtualFile vf) {
        this.project = project;
        this.vf = vf;

        this.allTestCases = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCases = Collections.synchronizedList(new ArrayList<>());

        this.resultsMap = new ConcurrentHashMap<>();

        buildOpeningPanel();
        loadDataAsync();

        FontSyncUtil.syncWithNativeEditor(project, list, this);
    }

    private void buildOpeningPanel() {
        toolBar = new RunToolBar(this);
        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        RunOpeningForm openingForm = new RunOpeningForm(toolBar, statusBar);
        mainPanel = openingForm.getMainPanel();
        list = openingForm.getList();
        model = openingForm.getModel();

        list.setCellRenderer(new RunListRenderer(this));

        final HoverListener hoverListener = new HoverListener(list, this);
        list.addMouseListener(hoverListener);
        list.addMouseMotionListener(hoverListener);

        final EditorCM editorCM = new EditorCM(this, vf.getTestRun(), list, model);
        final TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getTestRun(), editorCM);
        list.addMouseListener(testMouseListener);

        editorCM.registerShortcuts(this, vf.getTestRun(), list, model, editorCM);

        Path selectionPath = vf.getTestRun().getPath();
        list.addListSelectionListener(new SelectionListener(project, list, this, selectionPath));

        refreshView();
    }

    private void loadDataAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (this.tr == null && vf.getTestRun() != null) {
                    Path dirPath = vf.getTestRun().getPath();
                    Path jsonFilePath = dirPath.resolve(vf.getTestRun().getName() + ".json");

                    if (Files.exists(jsonFilePath)) {
                        this.tr = Mapper.readValue(jsonFilePath.toFile(), TestRunDto.class);
                    }
                }

                if (this.tr != null) {
                    Map<UUID, TestRunItems> newResults = this.tr.getResults().stream()
                            .collect(Collectors.toMap(
                                    TestRunItems::getId,
                                    item -> item,
                                    (existingItem, duplicateItem) -> existingItem
                            ));
                    this.resultsMap.putAll(newResults);
                }
            } catch (Exception e) {
                Log.error("Failed to load Test Run data from disk: " + e.getMessage());
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (this.list != null) {
                    this.list.setPaintBusy(true);
                    this.list.getEmptyText().setText("Loading...");
                }

                this.sessionCache = new RunSessionCache(this.tr);

                sessionCache.setListener(new RunSessionCache.ICacheListener() {
                    @Override
                    public void onItemsLoaded(final List<TestCaseDto> items) {
                        allTestCases.addAll(items);
                        currentTestCases.addAll(items);
                        items.forEach(item -> {
                            final TestRunItems runItem = resultsMap.get(item.getId());
                            if (runItem != null)
                                runItem.setTc(item);
                        });
                        refreshView();
                    }

                    @Override
                    public void onLoadComplete(final List<TestCaseDto> allItems) {
                        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                            final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(project, allItems).sortedList();
                            TestCaseCacheService.getInstance(project).load(sorted);

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
                    }
                });

                sessionCache.startLoadingAsync();
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
        if (list != null && model != null) {
            model.allContentsChanged();
        }
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        FilterPopup toolBarFilter = toolBar.getToolbarItem(FilterPopup.class);
        if (toolBarFilter != null)
            toolBarFilter.resetToolBarFilter();

        SearchTxt toolBarSearch = toolBar.getToolbarItem(SearchTxt.class);
        if (toolBarSearch != null)
            toolBarSearch.resetSearchQuery();

        if (sessionCache != null)
            sessionCache.dispose();

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

    public Set<?> getSelectedDetails() {
        AbstractToolbarPanel baseToolBar = getToolBar();
        if (baseToolBar != null) {
            RunDetailsPopup popup = baseToolBar.getToolbarItem(RunDetailsPopup.class);
            if (popup != null) {
                return popup.getSelectedDetails();
            }
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
            statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), total);
        }
    }

    @Override
    public Set<String> getAvailableModules() {
        if (this.sessionCache != null) {
            return this.sessionCache.getLoadedModules();
        }
        return Collections.emptySet();
    }

    private List<TestCaseDto> getFilteredList() {
        final String query = (toolBar != null && toolBar.getSearchTxt() != null)
                ? toolBar.getSearchTxt().getSearchQuery() : "";

        FilterPopup filterPopup = null;
        if (toolBar != null) {
            filterPopup = toolBar.getToolbarItem(FilterPopup.class);
        }

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
                        final boolean matchesGroup = groupFilter.isEmpty() || (groupFilter.contains(Group.UNASSIGNED) && tc.getGroup().isEmpty()) || (tc.getGroup().stream().anyMatch(groupFilter::contains));
                        final boolean matchesModule = moduleFilter.isEmpty() || moduleFilter.contains(tc.getModule());

                        return matchesSearch && matchesGroup && matchesPriority && matchesModule;
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void dispose() {
        if (list != null)
            for (MouseListener listener : list.getMouseListeners())
                list.removeMouseListener(listener);

        if (sessionCache != null) {
            sessionCache.dispose();
        }

        if (toolBar != null) {
            toolBar.dispose();
        }

        allTestCases.clear();
        resultsMap.clear();
        if (model != null) model.removeAll();
        if (mainPanel != null) mainPanel.removeAll();
        IEditorUI.super.dispose();
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

    private void startTimerForIndex(final int globalIndex) {
        if (globalIndex >= currentTestCases.size()) {
            stopExecution();
            if (tr != null) {
                tr.setStatus(TestRunStatus.COMPLETED);
                persistRunDataAsync();
            }
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
        }

        persistRunDataAsync();
        startTimerForIndex(currentlyExecutingIndex + 1);
    }

    public void handleManualStatusUpdate(final TestCaseDto tc, final TestStatus newStatus) {
        TestRunItems item = resultsMap.get(tc.getId());
        if (item != null) {
            item.setStatus(newStatus);
            item.setExecutedAt(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));

            int tcIndex = currentTestCases.indexOf(tc);
            if (tcIndex != -1 && tcIndex == currentlyExecutingIndex) {
                stopExecution();
            }

            if (list != null) {
                list.repaint();
            }

            persistRunDataAsync();
        }
    }

    private void persistRunDataAsync() {
        if (tr == null || vf.getTestRun() == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path dirPath = vf.getTestRun().getPath();
                Path jsonFilePath = dirPath.resolve(tr.getRunName());

                byte[] jsonBytes = Mapper.writeValueAsBytes(tr);
                Files.write(jsonFilePath, jsonBytes);

            } catch (Exception e) {
                Log.error("Failed to persist test run data: " + e.getMessage());
            }
        });
    }

    private void stopExecution() {
        if (executionTimer != null) {
            executionTimer.stop();
        }
        currentlyExecutingIndex = -1;
    }

    @Override
    public void onStartExecutionClicked() {
        if (tr != null && tr.getStatus() != TestRunStatus.IN_PROGRESS) {
            tr.setStatus(TestRunStatus.IN_PROGRESS);
            persistRunDataAsync();
        }

        startTimerForIndex(0);
    }
}