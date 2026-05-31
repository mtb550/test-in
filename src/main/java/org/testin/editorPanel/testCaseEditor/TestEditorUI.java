package org.testin.editorPanel.testCaseEditor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.actions.CreateTestCase;
import org.testin.editorPanel.EditorCM;
import org.testin.editorPanel.IEditorUI;
import org.testin.editorPanel.StatusBar;
import org.testin.editorPanel.UnifiedVirtualFile;
import org.testin.editorPanel.listeners.*;
import org.testin.editorPanel.toolBar.AbstractToolbarPanel;
import org.testin.editorPanel.toolBar.IToolBar;
import org.testin.editorPanel.toolBar.TestToolBar;
import org.testin.editorPanel.toolBar.components.FilterPopup;
import org.testin.editorPanel.toolBar.components.SearchTxt;
import org.testin.editorPanel.toolBar.components.TestDetailsPopup;
import org.testin.pojo.Config;
import org.testin.pojo.Group;
import org.testin.pojo.Priority;
import org.testin.pojo.TestEditorAttributes;
import org.testin.pojo.dto.TestCaseDto;
import org.testin.util.TestCaseSorter;
import org.testin.util.services.TestCaseCacheService;
import org.testin.viewPanel.ViewPanel;
import org.testin.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TestEditorUI implements Disposable, IToolBar, IEditorUI {
    @Getter
    private final UnifiedVirtualFile vf;

    private final JBPanel<?> mainPanel;

    private final JBList<TestCaseDto> list;

    private final CollectionListModel<TestCaseDto> model;

    private final ModelSyncListener syncListener;

    @Getter
    private final AbstractToolbarPanel toolBar;

    @Getter
    private final StatusBar statusBar;

    @Getter
    private final List<TestCaseDto> allTestCases;

    @Getter
    private final Set<UUID> unsortedIds;

    @Getter
    private final List<TestCaseDto> currentTestCases;

    private TestSessionCache sessionCache;

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

    public TestEditorUI(final @NotNull UnifiedVirtualFile vf) {
        this.vf = vf;

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

        final JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        this.pageSize = PropertiesComponent.getInstance().getInt("testin.pageSize", 50);

        this.toolBar = new TestToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        this.syncListener = new ModelSyncListener(this, model);
        this.syncListener.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(syncListener);

        final EditorCM editorCM = new EditorCM(this, vf.getTestSet(), list, model);
        final TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getTestSet(), editorCM);
        list.addMouseListener(testMouseListener);

        list.setTransferHandler(new TransferListener(this));
        list.setCellRenderer(new TestListRenderer(this));

        editorCM.registerShortcuts(this, vf.getTestSet(), list, model, editorCM);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);
        list.addListSelectionListener(new SelectionListener(list, this, vf.getTestSet().getPath()));

        final HoverListener hoverListener = new HoverListener(list, this);
        list.addMouseListener(hoverListener);
        list.addMouseMotionListener(hoverListener);

        list.addKeyListener(new KeyListener(list, this));

        loadDataAsync();
    }

    private void loadDataAsync() {
        this.sessionCache = new TestSessionCache(vf.getTestSet().getPath());

        sessionCache.setListener(new TestSessionCache.ICacheListener() {

            @Override
            public void onItemsLoaded(final List<TestCaseDto> items) {
                allTestCases.addAll(items);
                currentTestCases.addAll(items);
                items.forEach(item -> unsortedIds.add(item.getId()));
                items.forEach(item -> item.setPath(vf.getTestSet().getPath2()));
                refreshView();
            }

            @Override
            public void onLoadComplete(final List<TestCaseDto> allItems) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(allItems);
                    TestCaseCacheService.getInstance(Config.getProject()).load(result.sortedList());

                    ApplicationManager.getApplication().invokeLater(() -> {
                        allTestCases.clear();
                        allTestCases.addAll(result.sortedList());
                        currentTestCases.clear();
                        currentTestCases.addAll(result.sortedList());

                        unsortedIds.clear();
                        unsortedIds.addAll(result.unsortedIds());

                        if (list != null) {
                            list.setPaintBusy(false);
                            if (allTestCases.isEmpty()) {
                                list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
                            }
                        }

                        refreshView();
                    });
                });
            }
        });

        sessionCache.startLoadingAsync();
    }

    private void onDataSynced() {
        sortAndIdentifyUnsorted();
        refreshView();
    }

    public void updateSequenceAndSaveAll() {
        currentTestCases.clear();
        currentTestCases.addAll(getFilteredList());

        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCases) {
            snapshot = new ArrayList<>(this.allTestCases);
        }

        this.unsortedIds.clear();
        final Path dirPath = vf.getTestSet().getPath();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            for (int i = 0; i < snapshot.size(); i++) {
                TestCaseDto current = snapshot.get(i);
                current.setIsHead(i == 0);
                current.setNext(i < snapshot.size() - 1 ? snapshot.get(i + 1).getId() : null);

                try {
                    Config.getMapper().writerWithDefaultPrettyPrinter()
                            .writeValue(new File(dirPath.toFile(), current.getId() + ".json"), current);
                } catch (final Exception ignored) {
                }
            }
            ApplicationManager.getApplication().invokeLater(this::refreshView);
        });
    }

    public void selectTestCase(final TestCaseDto tc) {
        if (tc == null) return;

        if (!currentTestCases.contains(tc)) {
            FilterPopup popup = toolBar.getToolbarItem(FilterPopup.class);
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

        final VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(vf.getTestSet().getPath().toFile());
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
        CreateTestCase.execute(this, vf.getTestSet(), list, model);
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
        if (list != null && model != null) {
            model.allContentsChanged();
        }
    }

    @Override
    public void onToolBarRefreshButtonClicked() {
        FilterPopup toolBarFilter = toolBar.getToolbarItem(FilterPopup.class);
        if (toolBarFilter != null) {
            toolBarFilter.resetToolBarFilter();
        }

        SearchTxt toolBarSearch = toolBar.getToolbarItem(SearchTxt.class);
        if (toolBarSearch != null) {
            toolBarSearch.resetSearchQuery();
        }

        if (sessionCache != null) {
            sessionCache.dispose();
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
        if (baseToolBar != null) {
            TestDetailsPopup popup = baseToolBar.getToolbarItem(TestDetailsPopup.class);
            if (popup != null) {
                return popup.getSelectedDetails();
            }
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

        final TestCaseDto selectedItem = list != null ? list.getSelectedValue() : null;

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        if (selectedItem != null && pageItems.contains(selectedItem)) {
            list.setSelectedValue(selectedItem, true);
        }

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), totalItems);
    }

    private int getTotalPages(final List<TestCaseDto> filtered) {
        return filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / pageSize);
    }

    public void sortAndIdentifyUnsorted() {
        if (allTestCases.isEmpty()) return;

        synchronized (allTestCases) {
            final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(new ArrayList<>(allTestCases));

            this.allTestCases.clear();
            this.allTestCases.addAll(result.sortedList());

            this.unsortedIds.clear();
            this.unsortedIds.addAll(result.unsortedIds());
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
                        final boolean matchesGroup = groupFilter.isEmpty() || (groupFilter.contains(Group.UNASSIGNED) && (tc.getGroup().isEmpty())) || (tc.getGroup().stream().anyMatch(groupFilter::contains));
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

        if (list != null) {
            TestCaseDto selectedInThisFile = list.getSelectedValue();

            final ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
            if (viewer != null) {
                viewer.hide(selectedInThisFile);
            }
        }
        allTestCases.clear();
        currentTestCases.clear();
        unsortedIds.clear();

        if (model != null) {
            model.removeListDataListener(syncListener);
            model.removeAll();
        }
        if (mainPanel != null) mainPanel.removeAll();

        IEditorUI.super.dispose();
    }

    @Override
    public List<TestCaseDto> getSelectedTestCases() {
        return list != null ? list.getSelectedValuesList() : Collections.emptyList();
    }
}