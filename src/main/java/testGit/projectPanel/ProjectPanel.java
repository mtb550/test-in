package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.actions.OpenTestSet;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.projectSelector.ProjectSelector;
import testGit.projectPanel.testRunTab.MouseAdapterImpl;
import testGit.projectPanel.versionSelector.VersionSelector;
import testGit.util.DirectoryMapper;
import testGit.util.ShortcutRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;


@Getter
public class ProjectPanel {
    private final JPanel panel;
    private final SimpleTree testCaseTree;
    private final SimpleTree testRunTree;
    private final ProjectSelector projectSelector;
    private final VersionSelector versionSelector;
    private final JBTabsImpl tabs;

    public ProjectPanel(final Project project) {
        System.out.println("Panel.Panel()");
        panel = new JPanel(new BorderLayout());

        testCaseTree = new SimpleTree();
        testRunTree = new SimpleTree();

        projectSelector = new ProjectSelector(this);

        setupTestCaseTree();
        setupTestRunTree();

        JBScrollPane testCaseScrollPane = new JBScrollPane(testCaseTree);
        JBScrollPane testRunScrollPane = new JBScrollPane(testRunTree);

        JPanel topBar = new JPanel(new BorderLayout());

        topBar.add(projectSelector.selected(), BorderLayout.NORTH);

        Directory selectedProject = ProjectSelector.getSelectedProject();

        versionSelector = new VersionSelector(selectedProject);
        topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);

        tabs = new JBTabsImpl(project);

        TabInfo testCasesTab = new TabInfo(testCaseScrollPane)
                .setText("Test Cases")
                .setIcon(AllIcons.Nodes.Folder);

        TabInfo testRunsTab = new TabInfo(testRunScrollPane)
                .setText("Test Runs")
                .setIcon(AllIcons.Nodes.Artifact);

        tabs.addTab(testCasesTab);
        tabs.addTab(testRunsTab);

        Preferences prefs = Preferences.userRoot().node("TestGit");
        String lastTab = prefs.get("activeTab", "Test Cases");
        if ("Test Runs".equals(lastTab)) {
            tabs.select(testRunsTab, true);
        } else {
            tabs.select(testCasesTab, true);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                System.out.println("tabs.addListener.selectionChanged(): " + newSelection.getText());
                prefs.put("activeTab", newSelection.getText());
            }
        });

        panel.add(tabs.getComponent(), BorderLayout.CENTER);
    }

    public void setupTestCaseTree() {
        System.out.println("Panel.setupTestCaseTree()");

        DirectoryMapper.buildTestCasesTree();
        testCaseTree.setModel(DirectoryMapper.getTestCasesTreeModel());
        testCaseTree.setRootVisible(true);
        testCaseTree.setShowsRootHandles(true);
        testCaseTree.setCellRenderer(new Renderer());
        testCaseTree.addMouseListener(new testGit.projectPanel.testCaseTab.MouseAdapterImpl(this));
        Shortcuts.register(testCaseTree, Config.getProject());
        OpenTestSet.register(testCaseTree);
        testCaseTree.setDragEnabled(true);
        testCaseTree.setDropMode(DropMode.ON_OR_INSERT);
        testCaseTree.setTransferHandler(new TransferHandler(testCaseTree));
        ShortcutRegistry.Explorer(testCaseTree, this);
    }

    private void setupTestRunTree() {
        System.out.println("Panel.setupTestRunTree()");

        DirectoryMapper.buildTestRunsTree();
        testRunTree.setModel(DirectoryMapper.getTestRunsTreeModel());
        testRunTree.setRootVisible(true);
        testRunTree.addMouseListener(new MouseAdapterImpl(this));
        testRunTree.setShowsRootHandles(true);
        testRunTree.setCellRenderer(new Renderer());
        testRunTree.addTreeSelectionListener(e -> {
        });
    }

}