package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.pojo.Directory;
import testGit.projectPanel.projectSelector.ProjectSelector;
import testGit.projectPanel.testCaseTab.ShortcutHandler;
import testGit.projectPanel.testCaseTab.TestCaseRenderer;
import testGit.projectPanel.testRunTab.MouseAdapterImpl;
import testGit.projectPanel.testRunTab.TestRunRenderer;
import testGit.projectPanel.versionSelector.VersionSelector;
import testGit.util.TestCasesDirectoryMapper;
import testGit.util.TestRunsDirectoryMapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

@Getter
public class ProjectPanel implements Disposable {
    private final SimpleTree testCaseTree;
    private final SimpleTree testRunTree;
    private final JBPanel<?> panel;
    private final ProjectSelector projectSelector;
    private final VersionSelector versionSelector;
    private final JBTabs tabs;

    public ProjectPanel(final Project project) {
        projectSelector = new ProjectSelector(this);
        this.testCaseTree = new SimpleTree();
        this.testRunTree = new SimpleTree();
        panel = new JBPanel<>(new BorderLayout());

        JBScrollPane testCaseScrollPane = new JBScrollPane(testCaseTree);
        JBScrollPane testRunScrollPane = new JBScrollPane(testRunTree);

        JBPanel<?> topBar = new JBPanel<>(new BorderLayout());
        topBar.add(projectSelector.selected(), BorderLayout.NORTH);

        Directory selectedProject = ProjectSelector.getSelectedProject();
        versionSelector = new VersionSelector(selectedProject);
        topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);

        this.tabs = JBTabsFactory.createTabs(project, this);

        TabInfo testCasesTab = new TabInfo(testCaseScrollPane)
                .setText("Test Cases")
                .setIcon(AllIcons.Nodes.Folder);

        TabInfo testRunsTab = new TabInfo(testRunScrollPane)
                .setText("Test Runs")
                .setIcon(AllIcons.Nodes.Artifact);

        tabs.addTab(testCasesTab);
        tabs.addTab(testRunsTab);

        PropertiesComponent preference = PropertiesComponent.getInstance();
        String lastTab = preference.getValue("testGit.activeTab", "Test Cases");

        if ("Test Runs".equals(lastTab)) {
            tabs.select(testRunsTab, true);
        } else {
            tabs.select(testCasesTab, true);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                preference.setValue("testGit.activeTab", newSelection.getText());
            }
        });

        panel.add(tabs.getComponent(), BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
        testCaseTree.setModel(null);
        testRunTree.setModel(null);
    }

    public void setupTestCaseTree(Project project) {
        if (ProjectSelector.getSelectedProject() == null) {
            this.testCaseTree.getEmptyText().setText("Select a project to view test cases.");
            return;
        }

        this.testCaseTree.setRootVisible(true);
        this.testCaseTree.setShowsRootHandles(true);
        this.testCaseTree.setDragEnabled(true);
        this.testCaseTree.setDropMode(DropMode.ON_OR_INSERT);

        Set<DefaultMutableTreeNode> sharedCutNodes = new HashSet<>();
        this.testCaseTree.setCellRenderer(new TestCaseRenderer(sharedCutNodes));

        DumbService.getInstance(project).runWhenSmart(() ->
                TestCasesDirectoryMapper.buildTreeAsync(this.testCaseTree)
        );

        TransferHandlerImpl transferHandler = new TransferHandlerImpl(this.testCaseTree, sharedCutNodes);
        this.testCaseTree.setTransferHandler(transferHandler);
        ShortcutHandler.register(this, this.testCaseTree, transferHandler);
        this.testCaseTree.addMouseListener(new testGit.projectPanel.testCaseTab.MouseAdapterImpl(this));
    }

    public void setupTestRunTree(Project project) {
        DumbService.getInstance(project).runWhenSmart(() ->
                TestRunsDirectoryMapper.buildTreeAsync(this.testRunTree)
        );

        this.testRunTree.setRootVisible(true);
        this.testRunTree.setShowsRootHandles(true);
        testGit.projectPanel.testRunTab.ShortcutHandler.register(this.testRunTree);
        this.testRunTree.addMouseListener(new MouseAdapterImpl(this));
        this.testRunTree.setCellRenderer(new TestRunRenderer());
    }
}