package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import testGit.editorPanel.EditorCM;
import testGit.editorPanel.IEditorUI;
import testGit.editorPanel.StatusBar;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.editorPanel.listeners.*;
import testGit.editorPanel.toolBar.AbstractToolbarPanel;
import testGit.editorPanel.toolBar.IToolBar;
import testGit.editorPanel.toolBar.RunToolBar;
import testGit.editorPanel.toolBar.components.FilterPopup;
import testGit.editorPanel.toolBar.components.RunDetailsPopup;
import testGit.editorPanel.toolBar.components.SearchTxt;
import testGit.pojo.*;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;
import testGit.util.services.TestCaseCacheService;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseListener;
import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RunEditorUI implements Disposable, IToolBar, IEditorUI {

    private final UnifiedVirtualFile vf;

    @Getter
    private final List<TestCaseDto> allTestCaseDtos;

    @Getter
    private final List<TestCaseDto> currentTestCaseDtos;

    private final Set<UUID> initialTestCaseIds = new HashSet<>();

    private final VirtualFile currentFile;

    @Getter
    private final @NotNull Map<UUID, TestRunDto.TestRunItems> resultsMap;

    CheckboxTree checklistTree;

    TestRunDto metadata;

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

    private TestRunMetadataHeader metadataHeader;

    @Getter
    @Setter
    private String hoveredIconAction = null;

    @Getter
    @Setter
    private int hoveredIndex = -1;

    private Timer executionTimer;
    private long currentTestStartTime;
    private int currentlyExecutingIndex = -1;

    public RunEditorUI(final UnifiedVirtualFile vf) {
        this.vf = vf;

        this.allTestCaseDtos = Collections.synchronizedList(new ArrayList<>());
        this.currentTestCaseDtos = Collections.synchronizedList(new ArrayList<>());

        this.metadata = vf.getMetadata();
        this.currentFile = vf;

        if (this.metadata != null) {
            this.resultsMap = this.metadata.getResults().stream()
                    .collect(Collectors.toMap(TestRunDto.TestRunItems::getTestCaseId, item -> item));
        } else {
            this.resultsMap = new HashMap<>();
        }

        createEditorPanel();

        if (vf.getEditorType() == EditorType.TEST_RUN_OPENING) {
            loadDataAsync();
        }
    }

    private void loadDataAsync() {
        this.sessionCache = new RunSessionCache(metadata);

        sessionCache.setListener(new RunSessionCache.ICacheListener() {
            @Override
            public void onItemsLoaded(final List<TestCaseDto> items) {
                allTestCaseDtos.addAll(items);
                currentTestCaseDtos.addAll(items);
                items.forEach(item -> {
                    initialTestCaseIds.add(item.getId());
                    final TestRunDto.TestRunItems runItem = resultsMap.get(item.getId());
                    if (runItem != null)
                        runItem.setTestCaseDetails(item);
                });
                refreshView();
            }

            @Override
            public void onLoadComplete(final List<TestCaseDto> allItems) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(allItems).sortedList();
                    TestCaseCacheService.getInstance(Config.getProject()).load(sorted);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        allTestCaseDtos.clear();
                        allTestCaseDtos.addAll(sorted);

                        currentTestCaseDtos.clear();
                        currentTestCaseDtos.addAll(sorted);

                        initialTestCaseIds.clear();
                        initialTestCaseIds.addAll(sorted.stream().map(TestCaseDto::getId).collect(Collectors.toSet()));

                        if (list != null) {
                            list.setPaintBusy(false);
                            if (allTestCaseDtos.isEmpty()) {
                                list.getEmptyText().setText("No test cases found in this run.");
                            }
                        }

                        refreshView();
                    });
                });
            }
        });

        sessionCache.startLoadingAsync();
    }

    public void createEditorPanel() {
        switch (vf.getEditorType()) {
            case TEST_RUN_OPENING -> buildOpeningPanel();
            case TEST_RUN_CREATION ->
                    buildCreationPanel(vf.getTestCasesTreeModel(), vf.getDirectoryDto().getPath(), vf.getProjectPanel());
            default -> throw new IllegalArgumentException("Unsupported editor type: " + vf.getEditorType());
        }
    }

    private void buildOpeningPanel() {
        toolBar = new RunToolBar(this, this);

        model = new CollectionListModel<>();
        list = new JBList<>(model);

        list.setPaintBusy(true);
        list.getEmptyText().setText("Loading..");

        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RunListRenderer(this));

        final HoverListener hoverListener = new HoverListener(list, this);
        list.addMouseListener(hoverListener);
        list.addMouseMotionListener(hoverListener);

        final EditorCM editorCM = new EditorCM(this, vf.getDirectoryDto(), list, model);
        final TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getDirectoryDto(), editorCM);
        list.addMouseListener(testMouseListener);

        EditorCM.registerShortcuts(this, vf.getDirectoryDto(), list, model, editorCM);

        Path selectionPath = null;
        if (vf.getTestSet() != null) {
            selectionPath = vf.getTestSet().getPath();
        } else if (vf.getDirectoryDto() != null) {
            selectionPath = vf.getDirectoryDto().getPath();
        }
        list.addListSelectionListener(new SelectionListener(list, this, selectionPath));

        final JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());

        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        refreshView();
    }

    private void buildCreationPanel(final DefaultTreeModel testCaseModel, final Path savePath, final ProjectPanel projectPanel) {
        final CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

        mainPanel = new JBPanel<>(new BorderLayout());

        metadataHeader = new TestRunMetadataHeader();
        metadataHeader.setRunNameDisabled(vf.getDirectoryDto().getName());
        mainPanel.add(metadataHeader.getPanel(), BorderLayout.NORTH);

        checklistTree = new CheckboxTree(createTreeRenderer(), root,
                new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        TreeUtil.expandAll(checklistTree);

        mainPanel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);
        mainPanel.add(createSaveButton(root, savePath, projectPanel), BorderLayout.SOUTH);
    }

    private CheckboxTree.CheckboxTreeCellRenderer createTreeRenderer() {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(final @NotNull JTree tree, final @NotNull Object value, final boolean selected,
                                          final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
                if (!(value instanceof CheckedTreeNode node)) return;
                final Object userObj = node.getUserObject();

                if (userObj instanceof DirectoryDto dir) {
                    getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                } else if (userObj instanceof TestCaseDto tc) {
                    renderTestCaseNode(tc);
                } else if (userObj instanceof String str) {
                    getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }

            private void renderTestCaseNode(final TestCaseDto tc) {
                final @NotNull TestRunDto.TestRunItems result = resultsMap.get(tc.getId());
                if (result != null) {
                    final TestStatus status = result.getStatus();
                    getTextRenderer().append(tc.getDescription(), status.getStyle());
                    getTextRenderer().append(status.getDisplayText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    getTextRenderer().append(tc.getDescription(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        };
    }

    private JButton createSaveButton(final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel) {
        final JButton saveButton = new JButton("Save Test Run");
        saveButton.addActionListener(e -> {
            if (!metadataHeader.validate()) {
                JOptionPane.showMessageDialog(mainPanel, "Build number is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            saveButton.setEnabled(false);
            saveButton.setText("Saving...");

            metadataHeader.applyToMetadata(this.metadata);
            saveSelectedToJSON(root, savePath, projectPanel);
        });
        return saveButton;
    }

    private void saveSelectedToJSON(final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel) {
        final TestRunDto run = new TestRunDto();
        if (metadata != null) {
            run.setBuildNumber(metadata.getBuildNumber())
                    .setPlatform(metadata.getPlatform())
                    .setLanguage(metadata.getLanguage())
                    .setBrowser(metadata.getBrowser())
                    .setDeviceType(metadata.getDeviceType());
        }

        final String fileName = vf.getDirectoryDto().getName() + ".json";
        run.setRunName(fileName);
        run.setCreatedAt(LocalDateTime.now());
        run.setStatus(TestRunStatus.CREATED);

        final List<TestRunDto.TestRunItems> items = new ArrayList<>();
        final Map<Path, List<UUID>> pathMap = new HashMap<>();

        collectCheckedItems(root, items, pathMap);

        run.setResults(items);

        final List<TestRunDto.TestCase> testCasesPaths = new ArrayList<>();
        for (final Map.Entry<Path, List<UUID>> entry : pathMap.entrySet()) {
            final TestRunDto.TestCase tcPath = new TestRunDto.TestCase();
            tcPath.setPath(entry.getKey());
            tcPath.setUuid(entry.getValue());
            testCasesPaths.add(tcPath);
        }
        run.setTestCase(testCasesPaths);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Config.getMapper().writerWithDefaultPrettyPrinter().writeValue(new File(savePath.toFile(), fileName), run);

                ApplicationManager.getApplication().invokeLater(() -> {
                    projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
                    FileEditorManager.getInstance(Config.getProject()).closeFile(currentFile);
                });
            } catch (final Exception e) {
                e.printStackTrace(System.err);
            }
        });
    }

    private void collectCheckedItems(final CheckedTreeNode node, final List<TestRunDto.TestRunItems> items, final Map<Path, List<UUID>> pathMap) {
        if (node.getUserObject() instanceof TestCaseDto tc && node.isChecked()) {
            final TestRunDto.TestRunItems item = new TestRunDto.TestRunItems();
            item.setTestCaseId(tc.getId());
            item.setStatus(TestStatus.PENDING);
            final Object rootObj = ((DefaultMutableTreeNode) node.getRoot()).getUserObject();
            item.setProject(rootObj instanceof DirectoryDto d ? d.getName() : String.valueOf(rootObj));
            items.add(item);

            Path tcPath = null;
            TreeNode parent = node.getParent();
            while (parent != null) {
                if (parent instanceof DefaultMutableTreeNode pNode) {
                    if (pNode.getUserObject() instanceof DirectoryDto dir) {
                        tcPath = dir.getPath();
                        break;
                    }
                }
                parent = parent.getParent();
            }

            if (tcPath != null) {
                pathMap.computeIfAbsent(tcPath, k -> new ArrayList<>()).add(tc.getId());
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items, pathMap);
        }
    }

    private CheckedTreeNode convertToCheckedNodes(final DefaultMutableTreeNode node) {
        final Object userObj = node.getUserObject();
        final CheckedTreeNode newNode = new CheckedTreeNode(userObj);

        if (userObj instanceof TestCaseDto tc && initialTestCaseIds.contains(tc.getId())) {
            newNode.setChecked(true);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

    @Override
    public void onToolBarSearchValueChanged(final String query) {
        currentTestCaseDtos.clear();
        currentTestCaseDtos.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterSelectionChanged() {
        currentTestCaseDtos.clear();
        currentTestCaseDtos.addAll(getFilteredList());
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onToolBarFilterResetButtonClicked() {
        currentTestCaseDtos.clear();
        currentTestCaseDtos.addAll(getFilteredList());
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

        this.allTestCaseDtos.clear();
        this.currentTestCaseDtos.clear();
        this.initialTestCaseIds.clear();

        this.resultsMap.clear();
        if (this.metadata != null) {
            this.resultsMap.putAll(this.metadata.getResults().stream()
                    .collect(Collectors.toMap(TestRunDto.TestRunItems::getTestCaseId, item -> item)));
        }

        if (this.model != null) {
            this.model.removeAll();
        }

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
        return Math.max(1, (int) Math.ceil((double) currentTestCaseDtos.size() / pageSize));
    }

    @Override
    public int getTotalItemsCount() {
        return allTestCaseDtos.size();
    }

    @Override
    public void appendNewTestCase(final TestCaseDto tc) {
        this.allTestCaseDtos.add(tc);
        refreshView();
    }

    public void refreshView() {
        final int total = currentTestCaseDtos.size();
        final int totalPages = getTotalPageCount();
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        final int fromIndex = (currentPage - 1) * pageSize;
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<TestCaseDto> pageItems = currentTestCaseDtos.subList(fromIndex, toIndex);

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

    private List<TestCaseDto> getFilteredList() {
        final String query = (toolBar != null && toolBar.getSearchTxt() != null)
                ? toolBar.getSearchTxt().getSearchQuery() : "";

        FilterPopup filterPopup = null;
        if (toolBar != null) {
            filterPopup = toolBar.getToolbarItem(FilterPopup.class);
        }

        final Set<Group> groupFilter = filterPopup != null ? filterPopup.getSelectedGroup() : Collections.emptySet();
        final Set<Priority> priorityFilter = filterPopup != null ? filterPopup.getSelectedPriority() : Collections.emptySet();

        if (allTestCaseDtos.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (allTestCaseDtos) {
            return allTestCaseDtos.stream()
                    .filter(tc -> {
                        final boolean matchesSearch = query.isEmpty() || tc.getDescription().toLowerCase().contains(query) || tc.getId().toString().toLowerCase().contains(query) || tc.getExpectedResult().toLowerCase().contains(query) || tc.getSteps().stream().anyMatch(step -> step != null && step.toLowerCase().contains(query));
                        final boolean matchesPriority = priorityFilter.isEmpty() || priorityFilter.contains(tc.getPriority());
                        final boolean matchesGroup = groupFilter.isEmpty() || (groupFilter.contains(Group.UNASSIGNED) && tc.getGroup().isEmpty()) || (tc.getGroup().stream().anyMatch(groupFilter::contains));

                        return matchesSearch && matchesGroup && matchesPriority;
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

        allTestCaseDtos.clear();
        initialTestCaseIds.clear();
        resultsMap.clear();
        if (model != null) model.removeAll();
        if (mainPanel != null) mainPanel.removeAll();
        IEditorUI.super.dispose();
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
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

    private void startTimerForIndex(int index) {
        if (index >= currentTestCaseDtos.size()) {
            stopExecution();
            return;
        }

        currentlyExecutingIndex = index;
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);

        TestCaseDto currentTc = currentTestCaseDtos.get(index);
        TestRunDto.TestRunItems runItem = resultsMap.get(currentTc.getId());

        if (runItem == null) return;

        // تصفير الوقت عند البدء الجديد
        runItem.setDuration(Duration.ZERO);
        currentTestStartTime = System.currentTimeMillis();

        if (executionTimer != null) executionTimer.stop();

        executionTimer = new Timer(1000, e -> {
            // حساب الوقت المنقضي وتحديث الـ DTO مباشرة
            long seconds = (System.currentTimeMillis() - currentTestStartTime) / 1000;
            runItem.setDuration(Duration.ofSeconds(seconds));

            // إعادة رسم القائمة لتحديث قيمة الـ Duration الظاهرة في البطاقة
            list.repaint();
        });
        executionTimer.start();
    }

    // استدعاء هذه الدالة عند النقر على (PASS/FAIL)
    public void updateStatusAndNext(TestStatus status) {
        if (currentlyExecutingIndex == -1) return;

        TestCaseDto currentTc = currentTestCaseDtos.get(currentlyExecutingIndex);
        TestRunDto.TestRunItems item = resultsMap.get(currentTc.getId());

        if (item != null) {
            item.setStatus(status);
            item.setExecutedAt(LocalDateTime.now());
            // الـ duration تم تحديثه بالفعل بواسطة الـ timer
        }

        startTimerForIndex(currentlyExecutingIndex + 1);
    }

    private void stopExecution() {
        if (executionTimer != null) {
            executionTimer.stop();
        }
        currentlyExecutingIndex = -1;
        // يمكن إضافة تنبيه للمستخدم أو تحديث حالة الـ Toolbar
    }

    @Override
    public void onStartExecutionClicked() {
        // البدء من أول عنصر محدد، أو من البداية إذا لم يكن هناك تحديد
        int startIndex = list.getSelectedIndex() != -1 ? list.getSelectedIndex() : 0;

        // استدعاء الدالة التي قمنا ببرمجتها سابقاً
        startTimerForIndex(startIndex);
    }
}