package com.example.explorer;

//import com.example.editor.TestPlanEditor;
import com.example.pojo.TestPlan;
import com.example.pojo.Tree;
import com.example.util.ShortcutRegistry;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.NonOpaquePanel;
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
    private final SimpleTree tree;
    private final SimpleTree testPlanTree;

    public ExplorerPanel() {
        panel = new JPanel(new BorderLayout());
        tree = new SimpleTree();
        testPlanTree = new SimpleTree();

        // === Load Test Case Tree ===
        ExplorerTree.build();

        tree.setModel(ExplorerTree.getTreeModel());

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new IntelliJRenderer());
        tree.addMouseListener(new TestCaseTreeMouseAdapter(tree));
        TestCaseTreeKeyAdapter.register(tree, ProjectManager.getInstance().getOpenProjects()[0]);

        // enable drag and drop
        tree.setDragEnabled(true);
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new TreeTransferHandler(tree));

        // register all keyboard shortcuts
        ShortcutRegistry.Explorer(tree);

        // tree scroll view
        JBScrollPane scrollPane = new JBScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // === Load Test Plans Tree ===
        DefaultMutableTreeNode planRoot = new DefaultMutableTreeNode("Test Plans");
        TestPlan[] plans = new sql().get("SELECT * FROM nafath_tp_tree").as(TestPlan[].class);
        for (TestPlan plan : plans) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(plan);
            planRoot.add(node);
        }
        DefaultTreeModel planModel = new DefaultTreeModel(planRoot);
        testPlanTree.setModel(planModel);
        testPlanTree.setRootVisible(true);
        testPlanTree.setShowsRootHandles(true);
        testPlanTree.setCellRenderer(new IntelliJRenderer());
        testPlanTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) testPlanTree.getLastSelectedPathComponent();
            if (selected != null && selected.getUserObject() instanceof TestPlan plan && plan.getType() == 1) {
                //TestPlanEditor.open(plan.getId());
            }
        });
        testPlanTree.addMouseListener(new TestCaseTreeMouseAdapter(testPlanTree));
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);
        testPlanScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // === Header ===
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(new ComboBoxProjectSelector(this).getComponent(), BorderLayout.NORTH);

        // Switcher buttons panel
        JPanel switchers = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup switcherGroup = new ButtonGroup();

        JToggleButton btnTestCases = new JToggleButton("Test Cases");
        JToggleButton btnTestPlans = new JToggleButton("Test Plans");
        JToggleButton btnAutomation = new JToggleButton("Automation");

        switcherGroup.add(btnTestCases);
        switcherGroup.add(btnTestPlans);
        switcherGroup.add(btnAutomation);

        switchers.add(btnTestCases);
        switchers.add(btnTestPlans);
        switchers.add(btnAutomation);

        topBar.add(switchers, BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        // === Main Content Panel ===
        JPanel contentPanel = new JPanel(new CardLayout());
        contentPanel.add(scrollPane, "Test Cases");
        contentPanel.add(testPlanScrollPane, "Test Plans");
        contentPanel.add(new JLabel("Automation content coming soon..."), "Automation");

        panel.add(contentPanel, BorderLayout.CENTER);

        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();

        // === Remember last selected tab ===
        Preferences prefs = Preferences.userRoot().node("TestBind");
        String lastTab = prefs.get("activeTab", "Test Cases");

        btnTestCases.setSelected("Test Cases".equals(lastTab));
        btnTestPlans.setSelected("Test Plans".equals(lastTab));
        btnAutomation.setSelected("Automation".equals(lastTab));
        cardLayout.show(contentPanel, lastTab);

        btnTestCases.addActionListener(e -> {
            cardLayout.show(contentPanel, "Test Cases");
            prefs.put("activeTab", "Test Cases");
        });
        btnTestPlans.addActionListener(e -> {
            cardLayout.show(contentPanel, "Test Plans");
            prefs.put("activeTab", "Test Plans");
        });
        btnAutomation.addActionListener(e -> {
            cardLayout.show(contentPanel, "Automation");
            prefs.put("activeTab", "Automation");
        });
    }

    public void loadAllProjects() {
        ExplorerTree.build(); // Rebuild the whole tree
        tree.setModel(ExplorerTree.getTreeModel());
        tree.setRootVisible(true);
        //tree.expandRow(0);
    }

    public void filterByProject(int projectId) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");
        Tree selectedProject = new sql().get("SELECT * FROM tree WHERE id = ?", projectId).as(Tree.class);
        DefaultMutableTreeNode node = ExplorerTree.buildSubTree(selectedProject);
        root.add(node);

        ExplorerTree.treeModel = new DefaultTreeModel(root);
        tree.setModel(ExplorerTree.treeModel);
        tree.setRootVisible(true);
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
