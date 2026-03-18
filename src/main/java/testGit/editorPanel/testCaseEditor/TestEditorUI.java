package testGit.editorPanel.testCaseEditor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.*;
import testGit.pojo.Config;
import testGit.pojo.mappers.TestCase;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestEditorUI implements Disposable, ToolBar.Callbacks, BaseEditorUI {

    private final JBPanel<?> mainPanel;
    private final JBList<TestCase> list;
    private final CollectionListModel<TestCase> model;
    private final ModelSyncListener<TestCase> syncListener;
    private final ToolBar toolBar;
    private final StatusBar statusBar;

    private List<TestCase> allTestCases;

    @Getter
    private int currentPage = 1;
    @Getter
    private int pageSize = 10;

    public TestEditorUI(@NotNull UnifiedVirtualFile vf) {
        this.allTestCases = new ArrayList<>(vf.getTestCases());
        this.mainPanel = new JBPanel<>(new BorderLayout());

        pageSize = PropertiesComponent.getInstance().getInt("testGit.pageSize", 10);

        // Header — 'this' implements Callbacks so the header can notify us of changes
        this.toolBar = new ToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        // Model + sync listener
        this.model = new CollectionListModel<>(new ArrayList<>());
        this.syncListener = new ModelSyncListener<>(allTestCases, model);
        this.syncListener.setOnUpdate(() -> {
            toolBar.resetFilters();
            refreshView();
        });
        this.model.addListDataListener(syncListener);

        // List
        this.list = new JBList<>(model);
        list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.addListSelectionListener(new SelectionListener(list));

        EditorContextMenu editorContextMenu = new EditorContextMenu(vf.getTestSet(), list, model);
        TestMouseListener testMouseListener = new TestMouseListener(list, model, vf.getTestSet(), editorContextMenu);
        list.addMouseListener(testMouseListener);

        list.setTransferHandler(new TransferImpl(vf.getTestSet(), model, this::applyFilters));

        // 🌟 تمرير this إلى الريندرر (بما أن هذا الكلاس يملك دوال isShowGroups وغيرها)
        list.setCellRenderer(new TestListRenderer(this));

        EditorContextMenu.registerShortcuts(vf.getTestSet(), list, model);
        mainPanel.add(new JBScrollPane(list), BorderLayout.CENTER);

        // Status bar
        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        attachPaginationListeners();

        refreshView();
        EditorFocusSyncListener focusSyncListener = new EditorFocusSyncListener(this.list);

        // 🌟 تسجيل Listener آمن على مستوى المشروع واستخدام this للـ Disposable
        Config.getProject()
                .getMessageBus()
                .connect(this)
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        if (event.getNewFile() != null && event.getNewFile().equals(vf)) {
                            focusSyncListener.selectionChanged(event);
                        }
                    }
                });
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    // -------------------------------------------------------------------------
    // EditorHeader.Callbacks implementation
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Getters delegated to header — used by RendererImpl
    // -------------------------------------------------------------------------

    public boolean isShowGroups() {
        return toolBar.isShowGroups();
    }

    public boolean isShowPriority() {
        return toolBar.isShowPriority();
    }

    public Set<String> getSelectedDetails() {
        return toolBar.getSelectedDetails();
    }

    // -------------------------------------------------------------------------
    // Filtering and pagination
    // -------------------------------------------------------------------------

    public void applyFilters() {
        currentPage = 1;
        refreshView();
    }

    public void refreshView() {
        List<TestCase> filtered = getFilteredList();
        int totalItems = filtered.size();
        int totalPages = getTotalPages(filtered);
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        List<TestCase> pageItems = startIndex < totalItems
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), totalItems);
    }

    public void loadData(List<TestCase> loadedData) {
        this.allTestCases = loadedData;
        this.currentPage = 1;
        refreshView();
    }

    private List<TestCase> getFilteredList() {
        String query = toolBar.getSearchQuery();
        return allTestCases.stream()
                .filter(tc -> {
                    boolean matchesSearch = query.isEmpty() ||
                            (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query));
                    boolean matchesGroup = toolBar.getSelectedGroups().isEmpty() ||
                            (tc.getGroups() != null && tc.getGroups().stream().anyMatch(toolBar.getSelectedGroups()::contains));
                    return matchesSearch && matchesGroup;
                })
                .collect(Collectors.toList());
    }

    private int getTotalPages(List<TestCase> filtered) {
        return filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / pageSize);
    }

    private void attachPaginationListeners() {
        statusBar.getFirstButton().addActionListener(e -> {
            currentPage = 1;
            refreshView();
        });
        statusBar.getPrevButton().addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshView();
            }
        });
        statusBar.getNextButton().addActionListener(e -> {
            if (currentPage < getTotalPages(getFilteredList())) {
                currentPage++;
                refreshView();
            }
        });
        statusBar.getLastButton().addActionListener(e -> {
            currentPage = getTotalPages(getFilteredList());
            refreshView();
        });
        statusBar.getPageSizeField().addActionListener(e -> {
            try {
                int newSize = Integer.parseInt(statusBar.getPageSizeField().getText().trim());
                if (newSize > 0) {
                    pageSize = newSize;
                    currentPage = 1;
                    refreshView();
                }
            } catch (NumberFormatException ex) {
                statusBar.getPageSizeField().setText(String.valueOf(pageSize));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Disposal & memory management
    // -------------------------------------------------------------------------
    @Override
    public void dispose() {
        System.out.println("Disposing TestCaseEditorUI...");

        TestCase selectedInThisFile = list.getSelectedValue();
        ViewPanel.hideIfShowing(selectedInThisFile);

        if (model != null && syncListener != null) {
            model.removeListDataListener(syncListener);
        }

        if (model != null) {
            model.removeAll();
        }
        if (mainPanel != null) {
            mainPanel.removeAll();
        }
    }
}