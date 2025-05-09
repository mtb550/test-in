package com.example.explorer;

import com.example.pojo.TestPlan;
import com.example.pojo.Tree;
import com.example.util.ShortcutRegistry;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.prefs.Preferences;

@Getter
public class ExplorerPanel {
    private final JPanel panel;
    private final SimpleTree projectTree;
    private final SimpleTree testPlanTree;

    public ExplorerPanel() {
        panel = new JPanel(new BorderLayout());
        projectTree = new SimpleTree();
        testPlanTree = new SimpleTree();

        // === Load Test Case Tree ===
        ExplorerTree.build();

        projectTree.setModel(ExplorerTree.getTreeModel());

        projectTree.setRootVisible(true);
        projectTree.setShowsRootHandles(true);
        projectTree.setCellRenderer(new IntelliJRenderer());
        projectTree.addMouseListener(new TestCaseTreeMouseAdapter(projectTree));
        TestCaseTreeKeyAdapter.register(projectTree, ProjectManager.getInstance().getOpenProjects()[0]);

        // enable drag and drop
        projectTree.setDragEnabled(true);
        projectTree.setDropMode(DropMode.ON_OR_INSERT);
        projectTree.setTransferHandler(new TreeTransferHandler(projectTree));

        // register all keyboard shortcuts
        ShortcutRegistry.Explorer(projectTree);

        // tree scroll view
        JBScrollPane scrollPane = new JBScrollPane(projectTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // === Load Test Plans Tree ===
        DefaultMutableTreeNode planRoot = new DefaultMutableTreeNode("Test Plans");
        TestPlan[] plans = new sql().get("SELECT * FROM nafath_tp_tree").as(TestPlan[].class);
        for (TestPlan plan : plans) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(plan);
            planRoot.add(node);
        }
        DefaultTreeModel testPlanModel = new DefaultTreeModel(planRoot);
        testPlanTree.setModel(testPlanModel);
        testPlanTree.setRootVisible(true);
        testPlanTree.setShowsRootHandles(true);
        testPlanTree.setCellRenderer(new IntelliJRenderer());
        testPlanTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) testPlanTree.getLastSelectedPathComponent();
            if (selected != null && selected.getUserObject() instanceof TestPlan plan && plan.getType() == 1) {
                // TestPlanEditor.open(plan.getId());
            }
        });
        testPlanTree.addMouseListener(new TestCaseTreeMouseAdapter(testPlanTree));
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);
        testPlanScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // === Project Selector + Tabs ===
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(new ComboBoxProjectSelector(this).getComponent(), BorderLayout.NORTH);
        panel.add(topBar, BorderLayout.NORTH);

        // === IntelliJ-Style Tabbed Content ===
        JBTabs tabs = new JBTabsImpl(ProjectManager.getInstance().getOpenProjects()[0]);

        TabInfo testCaseTab = new TabInfo(scrollPane).setText("Test Cases").setIcon(AllIcons.Nodes.Folder);
        TabInfo testPlanTab = new TabInfo(testPlanScrollPane).setText("Test Plans").setIcon(AllIcons.Nodes.Artifact);
        //TabInfo automationTab = new TabInfo(new JLabel("Automation content coming soon...")).setText("Automation").setIcon(AllIcons.Nodes.Plugin);

        tabs.addTab(testCaseTab);
        tabs.addTab(testPlanTab);
        //tabs.addTab(automationTab);

        // === Persist tab selection ===
        Preferences prefs = Preferences.userRoot().node("TestBind");
        String lastTab = prefs.get("activeTab", "Test Cases");

        // Select previously selected tab
        switch (lastTab) {
            case "Test Plans" -> tabs.select(testPlanTab, true);
            //case "Automation" -> tabs.select(automationTab, true);
            default -> tabs.select(testCaseTab, true);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                prefs.put("activeTab", newSelection.getText());
            }
        });

        panel.add(tabs.getComponent(), BorderLayout.CENTER);
    }

    public void loadAllProjects() {
        ExplorerTree.build(); // Rebuild the whole tree
        projectTree.setModel(ExplorerTree.getTreeModel());
        projectTree.setRootVisible(true);
        //tree.expandRow(0);
    }

    public void filterByProject(int projectId) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");
        Tree selectedProject = new sql().get("SELECT * FROM tree WHERE id = ?", projectId).as(Tree.class);
        DefaultMutableTreeNode node = ExplorerTree.buildSubTree(selectedProject);
        root.add(node);

        ExplorerTree.treeModel = new DefaultTreeModel(root);
        projectTree.setModel(ExplorerTree.treeModel);
        projectTree.setRootVisible(true);
        //tree.expandRow(0);
    }

    static class IntelliJRenderer implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            SimpleColoredComponent comp = new SimpleColoredComponent();
            comp.setOpaque(false);
            comp.setBorder(null);

            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            String text = value.toString();

            if (userObject instanceof Tree projectNode) {
                text = projectNode.getName();
                switch (projectNode.getType()) {
                    case 0 -> comp.setIcon(AllIcons.Nodes.Project);
                    case 1 -> comp.setIcon(AllIcons.Nodes.Folder);
                    case 2 -> comp.setIcon(AllIcons.Nodes.Class);
                }
                if (TestCaseTreeKeyAdapter.isCutNode(projectNode.getId())) {
                    comp.append(text, SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }

            } else if (userObject instanceof TestPlan plan) {
                text = plan.getName();
                switch (plan.getType()) {
                    case 0 -> comp.setIcon(AllIcons.Nodes.Folder);
                    case 1 -> comp.setIcon(AllIcons.Nodes.Artifact);
                }
                comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            } else {
                comp.append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            return comp;
        }
    }


}
