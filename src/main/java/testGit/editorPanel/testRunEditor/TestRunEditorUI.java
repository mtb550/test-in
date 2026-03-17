package testGit.editorPanel.testRunEditor;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import testGit.editorPanel.StatusBar;
import testGit.editorPanel.ToolBar;
import testGit.pojo.*;
import testGit.pojo.mappers.TestCaseJsonMapper;
import testGit.pojo.mappers.TestRunJsonMapper;
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
public class TestRunEditorUI implements Disposable, ToolBar.Callbacks {

    // --- Shared ---
    private final VirtualFileImpl vf;
    private final List<TestCaseJsonMapper> initialTestCaseJsonMappers;
    private final Set<Integer> initialTestCaseUids;
    private JBPanel<?> mainPanel = new JBPanel<>(new BorderLayout());

    // --- Opening-mode state ---
    private TestRunCard selectedCard = null;
    private int currentPage = 1;
    private int pageSize = 10;
    private JPanel cardListPanel;
    private StatusBar statusBar;
    private ToolBar toolBar;  // only used in opening mode

    // --- Creation-mode state ---
    private CheckboxTree checklistTree;
    private TestRunJsonMapper metadata;
    private VirtualFile currentFile;
    private Map<UUID, TestRunJsonMapper.TestRunItems> resultsMap;
    private TestRunMetadataHeader metadataHeader;

    public TestRunEditorUI(VirtualFileImpl vf) {
        this.vf = vf;
        this.metadata = vf.getMetadata();
        this.currentFile = vf;

        List<TestCaseJsonMapper> cases = vf.getTestCaseJsonMappers() != null ? vf.getTestCaseJsonMappers() : Collections.emptyList();
        this.initialTestCaseJsonMappers = TestCaseSorter.sortTestCases(cases);
        this.initialTestCaseUids = this.initialTestCaseJsonMappers.stream()
                .map(TestCaseJsonMapper::getUid)
                .collect(Collectors.toSet());
    }

    public JComponent createEditorPanel() {
        return switch (vf.getEditorType()) {
            case TEST_RUN_OPENING -> buildOpeningPanel();
            case TEST_RUN_CREATION ->
                    buildCreationPanel(vf.getTestCasesTreeModel(), vf.getPkg().getPath(), vf.getProjectPanel());
            default -> throw new IllegalArgumentException("Unsupported editor type: " + vf.getEditorType());
        };
    }

    // -------------------------------------------------------------------------
    // EditorHeader.Callbacks  (opening mode only)
    // -------------------------------------------------------------------------

    @Override
    public void onFilterChanged() {
        currentPage = 1;
        renderPage();
    }

    @Override
    public void onDetailsChanged() {
        // renderPage() rebuilds every card from scratch, so detail visibility is applied automatically
        renderPage();
    }

    // -------------------------------------------------------------------------
    // Opening mode
    // -------------------------------------------------------------------------

    private JComponent buildOpeningPanel() {
        // Header — 'this' implements Callbacks
        toolBar = new ToolBar(this);

        cardListPanel = new JPanel();
        cardListPanel.setLayout(new BoxLayout(cardListPanel, BoxLayout.Y_AXIS));
        cardListPanel.setBackground(UIUtil.getTreeBackground());
        cardListPanel.setOpaque(true);

        JBScrollPane scrollPane = new JBScrollPane(cardListPanel);
        scrollPane.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        statusBar = new StatusBar();
        wirePaginationButtons();

        mainPanel = new JBPanel<>(new BorderLayout());
        mainPanel.add(toolBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        renderPage();
        return mainPanel;
    }

    private void renderPage() {
        // Sync pageSize from the StatusBar's text field in case the user edited it
        try {
            int parsed = Integer.parseInt(statusBar.getPageSizeField().getText().trim());
            if (parsed > 0) pageSize = parsed;
        } catch (NumberFormatException ignored) {
        }

        List<TestCaseJsonMapper> filtered = getFilteredList();
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<TestCaseJsonMapper> pageItems = filtered.subList(fromIndex, toIndex);

        cardListPanel.removeAll();
        for (int i = 0; i < pageItems.size(); i++) {
            TestRunCard card = new TestRunCard(fromIndex + i, pageItems.get(i));
            card.setSelectionListener(this::handleCardSelected);
            // Apply current header state to each freshly built card
            card.updateData(
                    fromIndex + i,
                    pageItems.get(i),
                    toolBar.isShowGroups(),
                    toolBar.isShowPriority(),
                    toolBar.getSelectedDetails()
            );
            cardListPanel.add(card);
        }
        cardListPanel.add(Box.createVerticalGlue());
        cardListPanel.revalidate();
        cardListPanel.repaint();

        // Scroll back to top after every page change
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, cardListPanel);
            if (sp != null) sp.getVerticalScrollBar().setValue(0);
        });

        statusBar.updatePaginationState(currentPage, totalPages, pageItems.size(), total);
        selectedCard = null;
    }

    /**
     * Returns the subset of initialTestCases that match the current search query
     * and group filter from the header.
     */
    private List<TestCaseJsonMapper> getFilteredList() {
        String query = toolBar != null ? toolBar.getSearchQuery() : "";
        Set<GroupType> groups = toolBar != null ? toolBar.getSelectedGroups() : Collections.emptySet();

        return initialTestCaseJsonMappers.stream()
                .filter(tc -> {
                    boolean matchesSearch = query.isEmpty() ||
                            (tc.getTitle() != null && tc.getTitle().toLowerCase().contains(query));
                    boolean matchesGroup = groups.isEmpty() ||
                            (tc.getGroups() != null && tc.getGroups().stream().anyMatch(groups::contains));
                    return matchesSearch && matchesGroup;
                })
                .collect(Collectors.toList());
    }

    private void wirePaginationButtons() {
        statusBar.getFirstButton().addActionListener(e -> {
            currentPage = 1;
            renderPage();
        });
        statusBar.getPrevButton().addActionListener(e -> {
            currentPage--;
            renderPage();
        });
        statusBar.getNextButton().addActionListener(e -> {
            currentPage++;
            renderPage();
        });
        statusBar.getLastButton().addActionListener(e -> {
            int total = getFilteredList().size();
            currentPage = Math.max(1, (int) Math.ceil((double) total / pageSize));
            renderPage();
        });
        statusBar.getPageSizeField().addActionListener(e -> {
            currentPage = 1;
            renderPage();
        });
    }

    private void handleCardSelected(TestRunCard newlySelected) {
        if (selectedCard != null && selectedCard != newlySelected) selectedCard.deselect();
        selectedCard = newlySelected;
    }

    // -------------------------------------------------------------------------
    // Creation mode
    // -------------------------------------------------------------------------

    private JComponent buildCreationPanel(DefaultTreeModel testCaseModel, Path savePath, ProjectPanel projectPanel) {
        CheckedTreeNode root = convertToCheckedNodes((DefaultMutableTreeNode) testCaseModel.getRoot());

        mainPanel = new JBPanel<>(new BorderLayout());

        metadataHeader = new TestRunMetadataHeader();
        mainPanel.add(metadataHeader.getPanel(), BorderLayout.NORTH);

        checklistTree = new CheckboxTree(createTreeRenderer(), root,
                new CheckboxTreeBase.CheckPolicy(true, true, true, true));
        TreeUtil.expandAll(checklistTree);

        mainPanel.add(new JBScrollPane(checklistTree), BorderLayout.CENTER);
        mainPanel.add(createSaveButton(root, savePath, projectPanel), BorderLayout.SOUTH);

        return mainPanel;
    }

    private CheckboxTree.CheckboxTreeCellRenderer createTreeRenderer() {
        return new CheckboxTree.CheckboxTreeCellRenderer() {
            @Override
            public void customizeRenderer(@NotNull JTree tree, @NotNull Object value, boolean selected,
                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                if (!(value instanceof CheckedTreeNode node)) return;
                Object userObj = node.getUserObject();

                if (userObj instanceof Directory dir) {
                    getTextRenderer().append(dir.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                } else if (userObj instanceof TestCaseJsonMapper tc) {
                    renderTestCaseNode(tc);
                } else if (userObj instanceof String str) {
                    getTextRenderer().append(str, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                }
            }

            private void renderTestCaseNode(TestCaseJsonMapper tc) {
                TestRunJsonMapper.TestRunItems result = findResultFor(tc.getId());
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

        if (userObj instanceof TestCaseJsonMapper tc && initialTestCaseUids.contains(tc.getUid())) {
            newNode.setChecked(true);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            newNode.add(convertToCheckedNodes((DefaultMutableTreeNode) node.getChildAt(i)));
        }
        return newNode;
    }

    private void saveSelectedToJSON(CheckedTreeNode root, Path savePath, ProjectPanel projectPanel) {
        TestRunJsonMapper run = new TestRunJsonMapper();
        if (metadata != null) {
            run.setBuildNumber(metadata.getBuildNumber());
            run.setPlatform(metadata.getPlatform());
            run.setLanguage(metadata.getLanguage());
            run.setBrowser(metadata.getBrowser());
            run.setDeviceType(metadata.getDeviceType());
        }

        String fileName = DirectoryType.TR.name() + "_" + metadata.getBuildNumber() + "_" + ProjectStatus.AC.name() + ".json";
        run.setRunName(fileName);
        run.setCreatedAt(LocalDateTime.now());
        run.setStatus(TestRunStatus.CREATED);

        List<TestRunJsonMapper.TestRunItems> items = new ArrayList<>();
        collectCheckedItems(root, items);
        run.setResults(items);

        try {
            Config.getMapper().writerWithDefaultPrettyPrinter().writeValue(new File(savePath.toFile(), fileName), run);
        } catch (Exception e) {
            System.err.println("Failed to save Test Run: " + e.getMessage());
            e.printStackTrace(System.err);
        }

        projectPanel.getTestRunTabController().buildTreeAsync(
                projectPanel.getTestProjectSelector().getSelectedTestProject().getItem());
        FileEditorManager.getInstance(Config.getProject()).closeFile(currentFile);
    }

    private void collectCheckedItems(CheckedTreeNode node, List<TestRunJsonMapper.TestRunItems> items) {
        if (node.getUserObject() instanceof TestCaseJsonMapper tc && node.isChecked()) {
            TestRunJsonMapper.TestRunItems item = new TestRunJsonMapper.TestRunItems();
            item.setTestCaseId(UUID.fromString(tc.getId()));
            item.setStatus("PENDING");
            Object rootObj = ((DefaultMutableTreeNode) node.getRoot()).getUserObject();
            item.setProject(rootObj instanceof Directory d ? d.getName() : String.valueOf(rootObj));
            items.add(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectCheckedItems((CheckedTreeNode) node.getChildAt(i), items);
        }
    }

    private TestRunJsonMapper.TestRunItems findResultFor(String testCaseId) {
        if (resultsMap == null) return null;
        try {
            return resultsMap.get(UUID.fromString(testCaseId));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void dispose() {
        System.out.println("Disposing TestRunEditorUI to free memory...");

        if (initialTestCaseJsonMappers != null) initialTestCaseJsonMappers.clear();
        if (initialTestCaseUids != null) initialTestCaseUids.clear();
        if (resultsMap != null) resultsMap.clear();

        if (mainPanel != null) {
            mainPanel.removeAll();
        }

        selectedCard = null;
    }
}
