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
import testGit.editorPanel.BaseEditorUI;
import testGit.editorPanel.EditorCM;
import testGit.editorPanel.StatusBar;
import testGit.editorPanel.UnifiedVirtualFile;
import testGit.editorPanel.listeners.*;
import testGit.editorPanel.toolBar.ToolBar;
import testGit.editorPanel.toolBar.ToolBarCallback;
import testGit.pojo.Config;
import testGit.pojo.EditorType;
import testGit.pojo.TestRunStatus;
import testGit.pojo.TestStatus;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;
import testGit.util.services.TestCaseCacheService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class RunEditorUI implements Disposable, ToolBarCallback, BaseEditorUI {

    private final UnifiedVirtualFile vf;
    private final List<TestCaseDto> initialTestCaseDtos = new ArrayList<>();
    private final Set<UUID> initialTestCaseIds = new HashSet<>();
    private final VirtualFile currentFile;
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
    private ToolBar toolBar;
    private TestRunMetadataHeader metadataHeader;

    @Getter
    @Setter
    private String hoveredIconAction = null;
    @Getter
    @Setter
    private int hoveredIndex = -1;

    public RunEditorUI(final UnifiedVirtualFile vf) {
        this.vf = vf;
        this.metadata = vf.getMetadata();
        this.currentFile = vf;

        if (this.metadata != null && this.metadata.getResults() != null) {
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

        sessionCache.setListener(new RunSessionCache.CacheListener() {
            @Override
            public void onItemsLoaded(final List<TestCaseDto> items) {
                initialTestCaseDtos.addAll(items);
                items.forEach(item -> initialTestCaseIds.add(item.getId()));
                refreshView();
            }

            @Override
            public void onLoadComplete(final List<TestCaseDto> allItems) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    final List<TestCaseDto> sorted = TestCaseSorter.sortTestCases(allItems).sortedList();
                    TestCaseCacheService.getInstance(Config.getProject()).load(sorted);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        initialTestCaseDtos.clear();
                        initialTestCaseDtos.addAll(sorted);

                        initialTestCaseIds.clear();
                        initialTestCaseIds.addAll(sorted.stream().map(TestCaseDto::getId).collect(Collectors.toSet()));

                        if (list != null) {
                            list.setPaintBusy(false);
                            if (initialTestCaseDtos.isEmpty()) {
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
        toolBar = new ToolBar(this);

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
                final TestStatus status = result.getStatus();
                getTextRenderer().append(tc.getTitle(), status.getStyle());
                getTextRenderer().append(status.getDisplayText(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
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
    public void onFilterChanged() {
        currentPage = 1;
        refreshView();
    }

    @Override
    public void onDetailsChanged() {
        if (list != null) {
            list.setFixedCellHeight(-1);
            list.setCellRenderer(new RunListRenderer(this));
            list.revalidate();
            list.repaint();
        }
    }

    public Set<String> getSelectedDetails() {
        return toolBar != null ? toolBar.getSettings().getSelectedDetails() : Collections.emptySet();
    }

    @Override
    public int getTotalPageCount() {
        return Math.max(1, (int) Math.ceil((double) getFilteredList().size() / pageSize));
    }

    @Override
    public int getTotalItemsCount() {
        return initialTestCaseDtos.size();
    }

    @Override
    public void appendNewTestCase(final TestCaseDto tc) {
        this.initialTestCaseDtos.add(tc);
        refreshView();
    }

    public void refreshView() {
        final List<TestCaseDto> filtered = getFilteredList();
        final int total = filtered.size();
        final int totalPages = getTotalPageCount();
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        final int fromIndex = (currentPage - 1) * pageSize;
        final int toIndex = Math.min(fromIndex + pageSize, total);
        final List<TestCaseDto> pageItems = filtered.subList(fromIndex, toIndex);

        model.replaceAll(pageItems);

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), total);
    }

    @Override
    public void dispose() {
        if (sessionCache != null) {
            sessionCache.dispose();
        }

        initialTestCaseDtos.clear();
        initialTestCaseIds.clear();
        resultsMap.clear();
        if (model != null) model.removeAll();
        if (mainPanel != null) mainPanel.removeAll();
        BaseEditorUI.super.dispose();
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        return list;
    }

    public @NotNull JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public List<TestCaseDto> getAllTestCaseDtos() {
        return initialTestCaseDtos;
    }

    @Override
    public void updateSequenceAndSaveAll() {
    }

    @Override
    public void selectTestCase(final TestCaseDto tc) {
        if (tc == null) return;
        final int index = model.getItems().indexOf(tc);
        if (index != -1) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    @Override
    public Set<UUID> getUnsortedIds() {
        return Collections.emptySet();
    }

    @Override
    public void onRefresh() {
        if (list != null) {
            list.revalidate();
            list.repaint();
        }
    }
}