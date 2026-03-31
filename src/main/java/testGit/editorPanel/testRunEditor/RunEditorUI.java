package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
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
import testGit.editorPanel.*;
import testGit.editorPanel.listeners.*;
import testGit.pojo.Config;
import testGit.pojo.Groups;
import testGit.pojo.TestRunStatus;
import testGit.pojo.dto.TestCaseDto;
import testGit.pojo.dto.TestRunDto;
import testGit.pojo.dto.dirs.DirectoryDto;
import testGit.projectPanel.ProjectPanel;
import testGit.util.TestCaseSorter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class RunEditorUI implements Disposable, ToolBar.Callbacks, BaseEditorUI {

    private final UnifiedVirtualFile vf;
    private final List<TestCaseDto> initialTestCaseDtos;
    private final Set<String> initialTestCaseIds;
    private JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

    private JBList<TestCaseDto> list;
    private CollectionListModel<TestCaseDto> model;
    private int currentPage = 1;
    private int pageSize = 10;
    private StatusBar statusBar;
    private ToolBar toolBar;

    private CheckboxTree checklistTree;
    private TestRunDto metadata;
    private VirtualFile currentFile;
    private Map<UUID, TestRunDto.TestRunItems> resultsMap;
    private TestRunMetadataHeader metadataHeader;

    @Getter
    @Setter
    private String hoveredIconAction = null;
    @Getter
    @Setter
    private int hoveredIndex = -1;

    public RunEditorUI(UnifiedVirtualFile vf) {
        this.vf = vf;
        this.metadata = vf.getMetadata();
        this.currentFile = vf;

        List<TestCaseDto> cases = vf.getTestCaseDtos() != null ? vf.getTestCaseDtos() : Collections.emptyList();
        this.initialTestCaseDtos = TestCaseSorter.sortTestCases(cases);
        this.initialTestCaseIds = this.initialTestCaseDtos.stream()
                .map(TestCaseDto::getId)
                .collect(Collectors.toSet());

        createEditorPanel();
    }

    public void createEditorPanel() {
        switch (vf.getEditorType()) {
            case TEST_RUN_OPENING -> buildOpeningPanel();
            case TEST_RUN_CREATION ->
                    buildCreationPanel(vf.getTestCasesTreeModel(), vf.getDirectoryDto().getPath(), vf.getProjectPanel());
            default -> throw new IllegalArgumentException("Unsupported editor type: " + vf.getEditorType());
        }
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

    public boolean isShowGroups() {
        return toolBar != null && toolBar.isShowGroups();
    }

    public boolean isShowPriority() {
        return toolBar != null && toolBar.isShowPriority();
    }

    public Set<String> getSelectedDetails() {
        return toolBar != null ? toolBar.getSelectedDetails() : Collections.emptySet();
    }

    @Override
    public int getTotalPageCount() {
        return Math.max(1, (int) Math.ceil((double) getFilteredList().size() / pageSize));
    }

    @Override
    public int getTotalItemsCount() {
        return initialTestCaseDtos != null ? initialTestCaseDtos.size() : 0;
    }

    @Override
    public void appendNewTestCase(TestCaseDto tc) {
        if (this.initialTestCaseDtos != null) {
            this.initialTestCaseDtos.add(tc);
            refreshView();
        }
    }

    private void buildOpeningPanel() {
        toolBar = new ToolBar(this);

        model = new CollectionListModel<>();
        list = new JBList<>(model);
        list.setOpaque(true);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new RunListRenderer(this));

        HoverListener hoverListener = new HoverListener(list, this);
        list.addMouseListener(hoverListener);
        list.addMouseMotionListener(hoverListener);

        EditorCM editorCM = new EditorCM(this, vf.getDirectoryDto(), list, model);
        TestMouseListener testMouseListener = new TestMouseListener(this, list, model, vf.getDirectoryDto(), editorCM);
        list.addMouseListener(testMouseListener);

        EditorCM.registerShortcuts(this, vf.getDirectoryDto(), list, model, editorCM);

        Path selectionPath = null;
        if (vf.getTestSet() != null) {
            selectionPath = vf.getTestSet().getPath();
        } else if (vf.getDirectoryDto() != null) {
            selectionPath = vf.getDirectoryDto().getPath();
        }
        list.addListSelectionListener(new SelectionListener(list, this, selectionPath));

        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setBorder(JBUI.Borders.empty());

        statusBar = new StatusBar();
        StatusBarListener.attach(this);

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        refreshView();
    }

    public void refreshView() {
        List<TestCaseDto> filtered = getFilteredList();
        int total = filtered.size();
        int totalPages = getTotalPageCount();
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TestCaseDto> pageItems = filtered.subList(fromIndex, toIndex);

        model.replaceAll(pageItems);

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), total);
    }

    private List<TestCaseDto> getFilteredList() {
        String query = toolBar != null ? toolBar.getSearchQuery() : "";
        Set<Groups> groups = toolBar != null ? toolBar.getSelectedGroups() : Collections.emptySet();

        return initialTestCaseDtos.stream()
                .filter(tc -> {
                    boolean matchesSearch = query.isEmpty() ||
                            (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query));
                    boolean matchesGroup = groups.isEmpty() ||
                            (tc.getGroups() != null && tc.getGroups().stream().anyMatch(groups::contains));
                    return matchesSearch && matchesGroup;
                })
                .collect(Collectors.toList());
    }

    private void buildCreationPanel(final DefaultTreeModel testCaseModel, final Path savePath, final ProjectPanel projectPanel) {
        CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

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
            public void customizeRenderer(@NotNull JTree tree, @NotNull Object value, boolean selected,
                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (!(value instanceof CheckedTreeNode node)) return;
                Object userObj = node.getUserObject();

                if (userObj instanceof DirectoryDto dir) {
                    getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                } else if (userObj instanceof TestCaseDto tc) {
                    renderTestCaseNode(tc);
                } else if (userObj instanceof String str) {
                    getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }

            private void renderTestCaseNode(TestCaseDto tc) {
                TestRunDto.TestRunItems result = findResultFor(tc.getId());
                SimpleTextAttributes style = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                String statusText = " [Pending]";

                if (result != null) {
                    switch (result.getStatus()) {
                        case "PASSED" -> {
                            style = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLUE);
                            statusText = " [Passed]";
                        }
                        case "FAILED" -> {
                            style = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.RED);
                            statusText = " [Failed]";
                        }
                        case "BLOCKED" -> {
                            style = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.ORANGE);
                            statusText = " [Blocked]";
                        }
                    }
                }
                getTextRenderer().append(tc.getTitle(), style);
                getTextRenderer().append(statusText, SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        };
    }

    private JButton createSaveButton(CheckedTreeNode root, Path savePath, ProjectPanel projectPanel) {
        JButton saveButton = new JButton("Save Test Run");
        saveButton.addActionListener(e -> {
            if (!metadataHeader.validate()) {
                JOptionPane.showMessageDialog(mainPanel, "Build number is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            metadataHeader.applyToMetadata(this.metadata);
            saveSelectedToJSON(root, savePath, projectPanel);
        });
        return saveButton;
    }

    private CheckedTreeNode convertToCheckedNodes(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        CheckedTreeNode newNode = new CheckedTreeNode(userObj);

        if (userObj instanceof TestCaseDto tc && initialTestCaseIds.contains(tc.getId())) {
            newNode.setChecked(true);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

    private void saveSelectedToJSON(final CheckedTreeNode root, final Path savePath, final ProjectPanel projectPanel) {
        TestRunDto run = new TestRunDto();
        if (metadata != null) {
            run.setBuildNumber(metadata.getBuildNumber())
                    .setPlatform(metadata.getPlatform())
                    .setLanguage(metadata.getLanguage())
                    .setBrowser(metadata.getBrowser())
                    .setDeviceType(metadata.getDeviceType());
        }

        String fileName = vf.getDirectoryDto().getName() + ".json";
        run.setRunName(fileName);
        run.setCreatedAt(LocalDateTime.now());
        run.setStatus(TestRunStatus.CREATED);

        List<TestRunDto.TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        try {
            Config.getMapper().writerWithDefaultPrettyPrinter().writeValue(new File(savePath.toFile(), fileName), run);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        projectPanel.getTestRunTreeBuilder().buildTree(projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
        FileEditorManager.getInstance(Config.getProject()).closeFile(currentFile);
    }

    private void collectCheckedItems(CheckedTreeNode node, List<TestRunDto.TestRunItems> items) {
        if (node.getUserObject() instanceof TestCaseDto tc && node.isChecked()) {
            TestRunDto.TestRunItems item = new TestRunDto.TestRunItems();
            item.setTestCaseId(UUID.fromString(tc.getId()));
            item.setStatus("PENDING");
            Object rootObj = ((DefaultMutableTreeNode) node.getRoot()).getUserObject();
            item.setProject(rootObj instanceof DirectoryDto d ? d.getName() : String.valueOf(rootObj));
            items.add(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }

    private TestRunDto.TestRunItems findResultFor(String testCaseId) {
        if (resultsMap == null) return null;
        try {
            return resultsMap.get(UUID.fromString(testCaseId));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void dispose() {
        if (initialTestCaseDtos != null) initialTestCaseDtos.clear();
        if (initialTestCaseIds != null) initialTestCaseIds.clear();
        if (resultsMap != null) resultsMap.clear();
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
    public void selectTestCase(TestCaseDto tc) {
        if (tc == null) return;
        int index = model.getItems().indexOf(tc);
        if (index != -1) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    @Override
    public Set<String> getUnsortedIds() {
        return Collections.emptySet();
    }
}