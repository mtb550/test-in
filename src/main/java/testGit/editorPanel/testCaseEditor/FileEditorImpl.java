package testGit.editorPanel.testCaseEditor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.EditorFocusSyncListener;
import testGit.editorPanel.StatusBar;
import testGit.editorPanel.ToolBar;
import testGit.pojo.Config;
import testGit.pojo.TestSet;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.viewPanel.ViewPanel;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileEditorImpl extends UserDataHolderBase implements FileEditor, ToolBar.Callbacks {

    private final JBPanel<?> panel;
    private final VirtualFile file;
    private final JBList<TestCaseJsonMapper> list;
    private final CollectionListModel<TestCaseJsonMapper> model;
    private final ModelSyncListener<TestCaseJsonMapper> syncListener;
    private final ToolBar toolBar;
    private final StatusBar statusBar;

    private List<TestCaseJsonMapper> allTestCaseJsonMappers;

    @Getter
    private int currentPage = 1;
    @Getter
    private int pageSize = 10;

    public FileEditorImpl(@NotNull List<TestCaseJsonMapper> testCaseJsonMappers, @NotNull TestSet dir, @NotNull VirtualFile file) {
        this.allTestCaseJsonMappers = new ArrayList<>(testCaseJsonMappers);
        this.panel = new JBPanel<>(new BorderLayout());
        this.file = file;

        pageSize = PropertiesComponent.getInstance().getInt("testGit.pageSize", 10);

        // Header — 'this' implements Callbacks so the header can notify us of changes
        this.toolBar = new ToolBar(this);
        panel.add(toolBar, BorderLayout.NORTH);

        // Model + sync listener
        this.model = new CollectionListModel<>(new ArrayList<>());
        this.syncListener = new ModelSyncListener<>(allTestCaseJsonMappers, model);
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
        list.addListSelectionListener(new SelectionListenerImpl(list));
        list.addMouseListener(new MouseAdapterImpl(list, model, dir));
        list.setTransferHandler(new TransferImpl(dir, model, this::applyFilters));
        list.setCellRenderer(new RendererImpl(this));
        ShortcutHandler.register(dir, list, model);
        panel.add(new JBScrollPane(list), BorderLayout.CENTER);

        // Status bar
        this.statusBar = new StatusBar();
        panel.add(statusBar, BorderLayout.SOUTH);
        attachPaginationListeners();

        refreshView();
        EditorFocusSyncListener syncListener = new EditorFocusSyncListener(this.list);

        Config.getProject()
                .getMessageBus()
                .connect(this)
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                    @Override
                    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                        if (event.getNewFile() != null && event.getNewFile().equals(file)) {
                            syncListener.selectionChanged(event);
                        }
                    }
                });
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
        // Force the cell renderer to re-measure row heights and repaint
        list.setFixedCellHeight(-1);
        list.setCellRenderer(new RendererImpl(this));
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
        List<TestCaseJsonMapper> filtered = getFilteredList();
        int totalItems = filtered.size();
        int totalPages = getTotalPages(filtered);
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        List<TestCaseJsonMapper> pageItems = startIndex < totalItems
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), totalItems);
    }

    public void loadData(List<TestCaseJsonMapper> loadedData) {
        this.allTestCaseJsonMappers = loadedData;
        this.currentPage = 1;
        refreshView();
    }

    private List<TestCaseJsonMapper> getFilteredList() {
        String query = toolBar.getSearchQuery();
        return allTestCaseJsonMappers.stream()
                .filter(tc -> {
                    boolean matchesSearch = query.isEmpty() ||
                            (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query));
                    boolean matchesGroup = toolBar.getSelectedGroups().isEmpty() ||
                            (tc.getGroups() != null && tc.getGroups().stream().anyMatch(toolBar.getSelectedGroups()::contains));
                    return matchesSearch && matchesGroup;
                })
                .collect(Collectors.toList());
    }

    private int getTotalPages(List<TestCaseJsonMapper> filtered) {
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
    // FileEditor boilerplate
    // -------------------------------------------------------------------------

    @Override
    public @NotNull VirtualFile getFile() {
        return file;
    }

    @Override
    public @NotNull JComponent getComponent() {
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    @Override
    public @NotNull String getName() {
        return "Test Case Editor";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void setState(@NotNull FileEditorState s) {
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener l) {
    }

    @Override
    public void dispose() {
        System.out.println("Disposing TestCase FileEditorImpl...");

        TestCaseJsonMapper selectedInThisFile = list.getSelectedValue();
        ViewPanel.hideIfShowing(selectedInThisFile);

        if (model != null && syncListener != null) {
            model.removeListDataListener(syncListener);
        }

        if (model != null) {
            model.removeAll();
        }
        if (panel != null) {
            panel.removeAll();
        }
    }
}
