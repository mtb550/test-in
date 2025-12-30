package com.example.explorer;

import com.example.pojo.Directory;
import com.example.pojo.TestPlan;
import com.example.util.ShortcutRegistry;
import com.example.util.sql;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
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

    // ✅ Add version selector field so it can be updated later
    private final ComboBoxVersionSelector versionSelector;

    public ExplorerPanel() {
        panel = new JPanel(new BorderLayout());

        projectTree = new SimpleTree();
        testPlanTree = new SimpleTree();

        setupTestCaseTree();
        setupTestPlanTree();

        // === Create Scroll Panes ===
        JBScrollPane testCaseScrollPane = new JBScrollPane(projectTree);
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);

        // === Project Selector Bar ===
        JPanel topBar = new JPanel(new BorderLayout());

        ComboBoxProjectSelector projectSelector = new ComboBoxProjectSelector(this);
        topBar.add(projectSelector.getComponent(), BorderLayout.NORTH);

        Directory selectedProject = ComboBoxProjectSelector.getSelectedProject();

        // ✅ Assign to field so we can refresh it on project change
        versionSelector = new ComboBoxVersionSelector(selectedProject);
        topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);

        // === Tabs ===
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        JBTabsImpl tabs = new JBTabsImpl(project);

        /*
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Test Case Notifications") // define this name once
                .createNotification("Test case TC-001 has been approved", NotificationType.INFORMATION)
                .notify(project);  // you must pass a valid `Project` instance

        NotificationGroupManager.getInstance()
                .getNotificationGroup("Test Case Notifications") // define this name once
                .createNotification("ARHBOWWWW our new friend SAAD!!", NotificationType.INFORMATION)
                .notify(project);  // you must pass a valid `Project` instance

         */


        TabInfo testCasesTab = new TabInfo(testCaseScrollPane)
                .setText("Test Cases")
                .setIcon(AllIcons.Nodes.Folder);

        TabInfo testPlansTab = new TabInfo(testPlanScrollPane)
                .setText("Test Plans")
                .setIcon(AllIcons.Nodes.Artifact);

        tabs.addTab(testCasesTab);
        tabs.addTab(testPlansTab);

        // === Tab selection persistence ===
        Preferences prefs = Preferences.userRoot().node("TestBind");
        String lastTab = prefs.get("activeTab", "Test Cases");
        if ("Test Plans".equals(lastTab)) {
            tabs.select(testPlansTab, true);
        } else {
            tabs.select(testCasesTab, true);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                prefs.put("activeTab", newSelection.getText());
            }
        });

        panel.add(tabs.getComponent(), BorderLayout.CENTER);
    }

    private void setupTestCaseTree() {
        ExplorerTree.buildTree();
        projectTree.setModel(ExplorerTree.getTreeModel());
        projectTree.setRootVisible(true);
        projectTree.setShowsRootHandles(true);
        projectTree.setCellRenderer(new IntelliJRenderer());
        projectTree.addMouseListener(new TestCaseTreeMouseAdapter(projectTree));
        TestCaseTreeKeyAdapter.register(projectTree, ProjectManager.getInstance().getOpenProjects()[0]);
        projectTree.setDragEnabled(true);
        projectTree.setDropMode(DropMode.ON_OR_INSERT);
        projectTree.setTransferHandler(new TreeTransferHandler(projectTree));
        ShortcutRegistry.Explorer(projectTree);
    }

    private void setupTestPlanTree() {
        DefaultMutableTreeNode planRoot = new DefaultMutableTreeNode("Test Plans");
        TestPlan[] plans = new sql().get("SELECT * FROM nafath_tp_tree").as(TestPlan[].class);
        for (TestPlan plan : plans) {
            planRoot.add(new DefaultMutableTreeNode(plan));
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
    }

    public void loadAllProjects() {
        ExplorerTree.buildTree();
        projectTree.setModel(ExplorerTree.getTreeModel());
        projectTree.setRootVisible(true);
    }

    public void filterByProject(Directory project) {
        // ✅ Refresh the version list dynamically
        if (versionSelector != null) {
            //versionSelector.setProjectId(projectId);
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Test Cases");
        DefaultMutableTreeNode node = ExplorerTree.buildSubTree(project);
        root.add(node);

        ExplorerTree.treeModel = new DefaultTreeModel(root);
        projectTree.setModel(ExplorerTree.treeModel);
        projectTree.setRootVisible(true);
        //projectTree.setCellRenderer(new IntelliJRenderer());
    }

    static class IntelliJRenderer extends SimpleColoredComponent implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            this.clear(); // مسح الحالة السابقة (ضروري جداً)

            Object userObject = null;
            if (value instanceof DefaultMutableTreeNode node) {
                userObject = node.getUserObject();
                System.out.println(userObject + "%%%");
                System.out.println(userObject.getClass() + "%%%");
            }

            // 1. حالة الـ Directory (المشاريع والمجلدات)
            if (userObject instanceof Directory projectNode) {
                // تعيين الأيقونة بناءً على النوع
                Icon icon = AllIcons.Nodes.Folder; // افتراضي
                if (projectNode.getType() != null) {
                    icon = switch (projectNode.getType()) {
                        case 0 -> AllIcons.Nodes.Project;
                        case 1 -> AllIcons.Nodes.Folder;
                        case 2 -> AllIcons.Nodes.Class;
                        default -> AllIcons.Nodes.Folder;
                    };
                }
                setIcon(icon); // ✅ تعيين الأيقونة

                // تعيين النص والستايل
                SimpleTextAttributes style = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                if (projectNode.getId() != null && TestCaseTreeKeyAdapter.isCutNode(projectNode.getId())) {
                    style = SimpleTextAttributes.GRAYED_ATTRIBUTES;
                }
                append(projectNode.getName(), style); // ✅ إضافة النص
            }

            // 2. حالة الـ TestPlan
            else if (userObject instanceof TestPlan plan) {
                Icon planIcon = (plan.getType() == 1) ? AllIcons.Nodes.Artifact : AllIcons.Nodes.Folder;
                setIcon(planIcon);
                append(plan.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            // 3. حالة النصوص العادية (مثل Root)
            else {
                setIcon(AllIcons.Nodes.Folder); // أيقونة اختيارية للـ Root
                append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

            return this;
        }
    }
}