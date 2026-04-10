package testGit.editorPanel.testCaseEditor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.*;
import testGit.editorPanel.listeners.*;
import testGit.pojo.Config;
import testGit.pojo.dto.TestCaseDto;
import testGit.util.services.TestCaseCacheService;
import testGit.viewPanel.ViewPanel;
import testGit.viewPanel.ViewToolWindowFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TestEditorUI implements Disposable, ToolBar.Callbacks, BaseEditorUI {

    private final JBPanel<?> mainPanel;
    private final JBList<TestCaseDto> list;
    private final CollectionListModel<TestCaseDto> model;
    private final ModelSyncListener syncListener;
    private final ToolBar toolBar;

    //private final TestFocusListener focusListener;

    @Getter
    private final UnifiedVirtualFile vf;

    @Getter
    private final StatusBar statusBar;

    @Getter
    private List<TestCaseDto> allTestCaseDtos;

    @Getter
    private Set<String> unsortedIds = new HashSet<>();

    @Getter
    @Setter
    private int currentPage = 1;

    @Getter
    @Setter
    private int pageSize = 50;

    @Getter
    @Setter
    private String hoveredIconAction = null;

    @Getter
    @Setter
    private int hoveredIndex = -1;

    public TestEditorUI(@NotNull UnifiedVirtualFile vf) {
        this.vf = vf;
        this.allTestCaseDtos = new ArrayList<>(vf.getTestCaseDtos());
        sortAndIdentifyUnsorted();
        TestCaseCacheService.getInstance(Config.getProject()).load(this.allTestCaseDtos);

        this.mainPanel = new JBPanel<>(new BorderLayout());
        this.pageSize = PropertiesComponent.getInstance().getInt("testGit.pageSize", 50);

        this.toolBar = new ToolBar(this);
        mainPanel.add(toolBar, BorderLayout.NORTH);

        this.model = new CollectionListModel<>(new ArrayList<>());
        this.syncListener = new ModelSyncListener(this, model);
        this.syncListener.setOnUpdateCallback(this::onDataSynced);
        this.model.addListDataListener(syncListener);

        this.list = new JBList<>(model);
        list.getEmptyText().setText("No test cases found").appendLine("Press Ctrl+M to add");
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);

        EditorCM editorCM = new EditorCM(this, vf.getTestSet(), list, model);
        TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getTestSet(), editorCM);
        list.addMouseListener(testMouseListener);

        list.setTransferHandler(new TransferListener(this));
        list.setCellRenderer(new TestListRenderer(this));

        EditorCM.registerShortcuts(this, vf.getTestSet(), list, model, editorCM);
        mainPanel.add(new JBScrollPane(list), BorderLayout.CENTER);

        this.statusBar = new StatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        StatusBarListener.attach(this);
        list.addListSelectionListener(new SelectionListener(list, this, vf.getTestSet().getPath()));

        refreshView();

        //this.focusListener = new TestFocusListener(this.list, vf);
        //this.focusListener.register();

        HoverListener hoverListener = new HoverListener(list, this);
        list.addMouseListener(hoverListener);
        list.addMouseMotionListener(hoverListener);
    }

    private void onDataSynced() {
        sortAndIdentifyUnsorted();
        refreshView();
    }

    public void updateSequenceAndSaveAll() {
        List<TestCaseDto> snapshot = new ArrayList<>(this.allTestCaseDtos);
        java.nio.file.Path dirPath = vf.getTestSet().getPath();

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < snapshot.size(); i++) {
                TestCaseDto current = snapshot.get(i);
                current.setIsHead(i == 0);
                current.setNext(i < snapshot.size() - 1 ? UUID.fromString(snapshot.get(i + 1).getId()) : null);

                try {
                    Config.getMapper().writerWithDefaultPrettyPrinter()
                            .writeValue(new File(dirPath.toFile(), current.getId() + ".json"), current);
                } catch (Exception ignored) {
                }
            }
        });
    }

    public void selectTestCase(TestCaseDto tc) {
        if (tc == null) return;

        List<TestCaseDto> filtered = getFilteredList();
        if (!filtered.contains(tc)) {
            toolBar.resetFilters();
            filtered = getFilteredList();
        }

        int index = filtered.indexOf(tc);
        if (index == -1) return;

        this.currentPage = (index / pageSize) + 1;
        refreshView();

        int localIndex = index % pageSize;
        SwingUtilities.invokeLater(() -> {
            list.setSelectedIndex(localIndex);
            list.ensureIndexIsVisible(localIndex);
            list.requestFocusInWindow();
        });
    }

    @Override
    public void appendNewTestCase(TestCaseDto tc) {
        this.allTestCaseDtos.add(tc);
        updateSequenceAndSaveAll();

        VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(vf.getTestSet().getPath().toFile());
        if (vDir != null) vDir.refresh(false, true);

        selectTestCase(tc);
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCaseDtos != null ? allTestCaseDtos.size() : 0;
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

    public boolean isShowGroups() {
        return toolBar.isShowGroups();
    }

    public boolean isShowPriority() {
        return toolBar.isShowPriority();
    }

    public Set<String> getSelectedDetails() {
        return toolBar.getSelectedDetails();
    }

    public void applyFilters() {
        currentPage = 1;
        refreshView();
    }

    public void refreshView() {
        List<TestCaseDto> filtered = getFilteredList();
        int totalItems = filtered.size();
        int totalPages = getTotalPages(filtered);
        if (currentPage > totalPages && totalPages > 0) currentPage = totalPages;

        int startIndex = (currentPage - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);
        List<TestCaseDto> pageItems = startIndex < totalItems
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        syncListener.pause();
        model.replaceAll(pageItems);
        syncListener.resume();

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), totalItems);
    }

    public void loadData(List<TestCaseDto> loadedData) {
        this.allTestCaseDtos = loadedData;
        sortAndIdentifyUnsorted();
        this.currentPage = 1;
        refreshView();
    }

    private List<TestCaseDto> getFilteredList() {
        String query = toolBar.getSearchQuery();
        return allTestCaseDtos.stream()
                .filter(tc -> {
                    boolean matchesSearch = query.isEmpty() ||
                            (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query));
                    boolean matchesGroup = toolBar.getSelectedGroups().isEmpty() ||
                            (tc.getGroups() != null && tc.getGroups().stream().anyMatch(toolBar.getSelectedGroups()::contains));
                    return matchesSearch && matchesGroup;
                })
                .collect(Collectors.toList());
    }

    private int getTotalPages(List<TestCaseDto> filtered) {
        return filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / pageSize);
    }

    public void sortAndIdentifyUnsorted() {
        if (allTestCaseDtos == null || allTestCaseDtos.isEmpty()) return;
        java.util.Map<String, TestCaseDto> map = allTestCaseDtos.stream().collect(Collectors.toMap(TestCaseDto::getId, tc -> tc));
        TestCaseDto head = allTestCaseDtos.stream().filter(tc -> Boolean.TRUE.equals(tc.getIsHead())).findFirst().orElse(null);

        List<TestCaseDto> sortedList = new ArrayList<>();
        Set<String> sortedIds = new HashSet<>();
        unsortedIds.clear();

        TestCaseDto current = head;
        while (current != null && !sortedIds.contains(current.getId())) {
            sortedList.add(current);
            sortedIds.add(current.getId());
            current = current.getNext() != null ? map.get(current.getNext().toString()) : null;
        }

        for (TestCaseDto tc : allTestCaseDtos) {
            if (!sortedIds.contains(tc.getId())) {
                sortedList.add(tc);
                unsortedIds.add(tc.getId());
            }
        }
        this.allTestCaseDtos.clear();
        this.allTestCaseDtos.addAll(sortedList);
    }

    @Override
    public void dispose() {
        //if (focusListener != null) {
        //focusListener.disconnect();
        //}

        TestCaseDto selectedInThisFile = list.getSelectedValue();

        ViewPanel viewer = ViewToolWindowFactory.getViewPanel();
        if (viewer != null) {
            viewer.hide(selectedInThisFile);
        }

        if (model != null && syncListener != null) model.removeListDataListener(syncListener);
        if (model != null) model.removeAll();
        if (mainPanel != null) mainPanel.removeAll();
        BaseEditorUI.super.dispose();
    }
}