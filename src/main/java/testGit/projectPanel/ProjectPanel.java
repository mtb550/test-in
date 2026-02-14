package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.actions.OpenFeatureAction;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.projectSelector.ProjectSelector;
import testGit.projectPanel.testPlanTab.MouseAdapter;
import testGit.projectPanel.versionSelector.VersionSelector;
import testGit.util.DirectoryMapper;
import testGit.util.ShortcutRegistry;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.prefs.Preferences;


@Getter
public class ProjectPanel {
    private final JPanel panel;
    private final SimpleTree testCaseTree;
    private final SimpleTree testPlanTree;
    private final ProjectSelector projectSelector;
    private final VersionSelector versionSelector;
    private final JBTabsImpl tabs;

    public ProjectPanel(final Project project) {
        System.out.println("Panel.Panel()");
        panel = new JPanel(new BorderLayout());

        testCaseTree = new SimpleTree();
        testPlanTree = new SimpleTree();

        projectSelector = new ProjectSelector(this);

        setupTestCaseTree();
        setupTestPlanTree();

        JBScrollPane testCaseScrollPane = new JBScrollPane(testCaseTree);
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);

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

        TabInfo testPlansTab = new TabInfo(testPlanScrollPane)
                .setText("Test Plans")
                .setIcon(AllIcons.Nodes.Artifact);

        tabs.addTab(testCasesTab);
        tabs.addTab(testPlansTab);

        Preferences prefs = Preferences.userRoot().node("TestGit");
        String lastTab = prefs.get("activeTab", "Test Cases");
        if ("Test Plans".equals(lastTab)) {
            tabs.select(testPlansTab, true);
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
        //testCaseTree.setRootVisible(false);
        testCaseTree.setShowsRootHandles(true);
        testCaseTree.setCellRenderer(new Renderer());
        testCaseTree.addMouseListener(new testGit.projectPanel.testCaseTab.MouseAdapter(this));
        Shortcuts.register(testCaseTree, Config.getProject());
        OpenFeatureAction.register(testCaseTree);
        testCaseTree.setDragEnabled(true);
        testCaseTree.setDropMode(DropMode.ON_OR_INSERT);
        testCaseTree.setTransferHandler(new TransferHandler(testCaseTree));
        ShortcutRegistry.Explorer(testCaseTree, this);
    }

    private void setupTestPlanTree() {
        System.out.println("Panel.setupTestPlanTree()");

        DirectoryMapper.buildTestPlansTree();
        testPlanTree.setModel(DirectoryMapper.getTestPlansTreeModel());
        //testPlanTree.setRootVisible(false);
        testPlanTree.addMouseListener(new MouseAdapter(this));
        testPlanTree.setShowsRootHandles(true);
        testPlanTree.setCellRenderer(new Renderer());
        testPlanTree.addTreeSelectionListener(e -> {
        });
    }

    public void filterByProject(final Directory project) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        if (project.getName().equals("All Projects")) {
            DirectoryMapper.buildTestCasesTree();
            DirectoryMapper.buildTestPlansTree();

            testCaseTree.setModel(DirectoryMapper.getTestCasesTreeModel());
            testPlanTree.setModel(DirectoryMapper.getTestPlansTreeModel());

            testCaseTree.setRootVisible(false);
            testPlanTree.setRootVisible(false);

        } else {
            DefaultMutableTreeNode casesRoot = DirectoryMapper.buildNodeRecursive(project, "testCases");
            DefaultMutableTreeNode plansRoot = DirectoryMapper.buildNodeRecursive(project, "testPlans");

            DirectoryMapper.setTestCasesTreeModel(new DefaultTreeModel(casesRoot));
            DirectoryMapper.setTestPlansTreeModel(new DefaultTreeModel(plansRoot));

            testCaseTree.setModel(DirectoryMapper.getTestCasesTreeModel());
            testPlanTree.setModel(DirectoryMapper.getTestPlansTreeModel());

            testCaseTree.setRootVisible(true);
            testPlanTree.setRootVisible(true);
        }

        testCaseTree.revalidate();
        testPlanTree.revalidate();
        testCaseTree.repaint();

        //expandAllNodes(testCaseTree);
        //expandAllNodes(testPlanTree);
    }

}