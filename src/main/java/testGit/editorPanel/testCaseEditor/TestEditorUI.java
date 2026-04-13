package testGit.editorPanel.testCaseEditor;

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
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.EditorCM;
import testGit.editorPanel.StatusBar;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.editorPanel.listeners.*;
import testGit.editorPanel.toolBar.ToolBar;
import testGit.editorPanel.toolBar.ToolBarCallback;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.TestCaseSorter;
import testGit.util.services.TestCaseCacheService;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class TestEditorUI implements Disposable, ToolBarCallback, BaseEditorUI {

    private final JBPanel<?> mainPanel;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final ModelSyncListener syncListener;

    @Getter
    private final ToolBar toolBar;

    @Getter
    private final UnifiedVirtualFile vf;

    @Getter
    private final StatusBar statusBar;

    @Getter
    private final List<TestCaseDto> allTestCaseDtos;

    @Getter
    private final Set<UUID> unsortedIds;

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
        this.allTestCaseDtos = Collections.synchronizedList(new ArrayList<>());
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

        this.pageSize = PropertiesComponent.getInstance().getInt("testGit.pageSize", 50);

        this.toolBar = new ToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        this.syncListener = new ModelSyncListener(this, model);
        this.syncListener.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(syncListener);

        final EditorCM editorCM = new EditorCM(this, vf.getTestSet(), list, model);
        final TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getTestSet(), editorCM);
        list.addMouseListener(testMouseListener);

        list.setTransferHandler(new TransferListener(this));
        list.setCellRenderer(new TestListRenderer(this));

        EditorCM.registerShortcuts(this, vf.getTestSet(), list, model, editorCM);
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

        sessionCache.setListener(new TestSessionCache.CacheListener() {

            @Override
            public void onItemsLoaded(final List<TestCaseDto> items) {
                allTestCaseDtos.addAll(items);
                items.forEach(item -> unsortedIds.add(item.getId()));
                refreshView();
            }

            @Override
            public void onLoadComplete(final List<TestCaseDto> allItems) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(allItems);
                    TestCaseCacheService.getInstance(Config.getProject()).load(result.sortedList());

                    ApplicationManager.getApplication().invokeLater(() -> {
                        allTestCaseDtos.clear();
                        allTestCaseDtos.addAll(result.sortedList());
                        unsortedIds.clear();
                        unsortedIds.addAll(result.unsortedIds());

                        list.setPaintBusy(false);
                        if (allTestCaseDtos.isEmpty()) {
                            list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
                        }

//                        if (!unsortedIds.isEmpty()) {
//                            updateSequenceAndSaveAll();
//                        } else {
                        refreshView();
                        //}
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
        final List<TestCaseDto> snapshot;
        synchronized (this.allTestCaseDtos) {
            snapshot = new ArrayList<>(this.allTestCaseDtos);
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

        List<TestCaseDto> filtered = getFilteredList();
        if (!filtered.contains(tc)) {
            toolBar.resetFilters();
            filtered = getFilteredList();
        }

        final int index = filtered.indexOf(tc);
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
        this.allTestCaseDtos.add(tc);
        //sortAndIdentifyUnsorted();
        updateSequenceAndSaveAll();

        final VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(vf.getTestSet().getPath().toFile());
        if (vDir != null) vDir.refresh(false, true);

        selectTestCase(tc);
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCaseDtos.size();
    }

    @Override
    public int getTotalPageCount() {
        return getTotalPages(getFilteredList());
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public void onFilterChanged() {
        applyFilters();
    }

    @Override
    public void onDetailsChanged() {
        list.setFixedCellHeight(-1);
        list.setCellRenderer(new TestListRenderer(this));
        list.revalidate();
        list.repaint();
    }

    public Set<String> getSelectedDetails() {
        return toolBar.getSettings().getSelectedDetails();
    }

    public void applyFilters() {
        currentPage = 1;
        refreshView();
    }

    public void refreshView() {
        final List<TestCaseDto> filtered = getFilteredList();
        final int totalItems = filtered.size();
        final int totalPages = getTotalPages(filtered);
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        final int startIndex = (currentPage - 1) * pageSize;
        final int endIndex = Math.min(startIndex + pageSize, totalItems);
        final List<TestCaseDto> pageItems = startIndex < totalItems
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), totalItems);
    }

    private int getTotalPages(final List<TestCaseDto> filtered) {
        return filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / pageSize);
    }

    public void sortAndIdentifyUnsorted() {
        if (allTestCaseDtos.isEmpty()) return;

        synchronized (allTestCaseDtos) {
            final TestCaseSorter.SortResult result = TestCaseSorter.sortTestCases(new ArrayList<>(allTestCaseDtos));

            this.allTestCaseDtos.clear();
            this.allTestCaseDtos.addAll(result.sortedList());

            this.unsortedIds.clear();
            this.unsortedIds.addAll(result.unsortedIds());
        }
    }

    @Override
    public void dispose() {
        if (sessionCache != null) {
            sessionCache.dispose();
        }

        final TestCaseDto selectedInThisFile = list.getSelectedValue();

        final ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
        if (viewer != null) {
            viewer.hide(selectedInThisFile);
        }

        allTestCaseDtos.clear();
        unsortedIds.clear();

        if (model != null) {
            model.removeListDataListener(syncListener);
            model.removeAll();
        }
        if (mainPanel != null) mainPanel.removeAll();

        BaseEditorUI.super.dispose();
    }

    @Override
    public void onRefresh() {
        if (sessionCache != null) {
            sessionCache.dispose();
        }

        this.allTestCaseDtos.clear();
        this.unsortedIds.clear();
        this.model.removeAll();
        this.list.setPaintBusy(true);
        this.list.getEmptyText().setText("Refreshing...");

        loadDataAsync();
    }
}