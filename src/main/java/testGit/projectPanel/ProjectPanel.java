package testGit.projectPanel;

import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.TabsListener;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import testGit.actions.OpenFeatureAction;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.pojo.TestPlan;
import testGit.projectPanel.testCase.TestCaseMouseAdapter;
import testGit.util.ShortcutRegistry;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.prefs.Preferences;


@Getter
public class ProjectPanel {
    private final JPanel panel;
    private final SimpleTree testCaseTree;
    private final SimpleTree testPlanTree;
    private final ComboBoxProjectSelector projectSelector;
    private final ComboBoxVersionSelector versionSelector;
    private final JBTabsImpl tabs;

    public ProjectPanel(final Project project) {
        System.out.println("Panel.Panel()");
        panel = new JPanel(new BorderLayout());

        testCaseTree = new SimpleTree();
        testPlanTree = new SimpleTree();

        projectSelector = new ComboBoxProjectSelector(this);

        setupTestCaseTree();
        setupTestPlanTree();

        // === Create Scroll Panes ===
        JBScrollPane testCaseScrollPane = new JBScrollPane(testCaseTree);
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);

        // === Project Selector Bar ===
        JPanel topBar = new JPanel(new BorderLayout());


        topBar.add(projectSelector.selected(), BorderLayout.NORTH);

        Directory selectedProject = ComboBoxProjectSelector.getSelectedProject();

        // ✅ Assign to field so we can refresh it on project change
        versionSelector = new ComboBoxVersionSelector(selectedProject);
        topBar.add(versionSelector.getComponent(), BorderLayout.SOUTH);

        panel.add(topBar, BorderLayout.NORTH);

        // === Tabs ===
        tabs = new JBTabsImpl(project);

        NotificationGroupManager.getInstance()
                .getNotificationGroup("Test Case Notifications") // define this name once
                .createNotification("Test case TC-001 has been approved", NotificationType.INFORMATION)
                .notify(project);  // you must pass a valid `Project` instance

        NotificationGroupManager.getInstance()
                .getNotificationGroup("Test Case Notifications") // define this name once
                .createNotification("ARHBOWWWW our new friend SAAD!!", NotificationType.INFORMATION)
                .notify(project);  // you must pass a valid `Project` instance


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
                System.out.println("tabs.addListener.selectionChanged(): " + newSelection.getText());
                prefs.put("activeTab", newSelection.getText());
            }
        });

        panel.add(tabs.getComponent(), BorderLayout.CENTER);
    }

    public void setupTestCaseTree() {
        System.out.println("Panel.setupTestCaseTree()");

        DirectoryMapper.buildTestCasesTree(ComboBoxProjectSelector.getSelectedProject());
        testCaseTree.setModel(DirectoryMapper.getTestCasesTreeModel());
        testCaseTree.setRootVisible(false);
        testCaseTree.setShowsRootHandles(true);
        testCaseTree.setCellRenderer(new IntelliJRenderer());
        testCaseTree.addMouseListener(new TestCaseMouseAdapter(this));
        Shortcuts.register(testCaseTree, Config.getProject());
        OpenFeatureAction.register(testCaseTree);
        testCaseTree.setDragEnabled(true);
        testCaseTree.setDropMode(DropMode.ON_OR_INSERT);
        testCaseTree.setTransferHandler(new TreeTransferHandler(testCaseTree));
        ShortcutRegistry.Explorer(testCaseTree, this);
    }

    private void setupTestPlanTree() {
        System.out.println("Panel.setupTestPlanTree()");

        DirectoryMapper.buildTestPlansTree(ComboBoxProjectSelector.getSelectedProject());
        testPlanTree.setModel(DirectoryMapper.getTestPlansTreeModel());
        testPlanTree.setRootVisible(false);
        testPlanTree.setShowsRootHandles(true);
        testPlanTree.setCellRenderer(new IntelliJRenderer());
        testPlanTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) testPlanTree.getLastSelectedPathComponent();
            //if (selected != null && selected.getUserObject() instanceof TestPlan plan && plan.getType() == 1) {
            // TestPlanEditor.open(plan.getId());
            //}
        });
        testPlanTree.addMouseListener(new TestCaseMouseAdapter(this));
    }

    public void refreshTestCaseTree() {
        System.out.println("Panel.refreshTestCaseTree()");

        DirectoryMapper.buildTestCasesTree(ComboBoxProjectSelector.getSelectedProject());
        DirectoryMapper.buildTestPlansTree(ComboBoxProjectSelector.getSelectedProject());
        testCaseTree.setModel(DirectoryMapper.getTestCasesTreeModel());
        testPlanTree.setModel(DirectoryMapper.getTestPlansTreeModel());
    }

    public void refreshTestPlanTree() {
        System.out.println("Panel.refreshTestPlanTree()");

    }

    public void refreshProjects() {
        System.out.println("Panel.refreshProjects()");

    }

    public void filterByProject(final Directory project) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        //refreshPath(project.getFilePath());

        // ✅ Refresh the version list dynamically
        //if (versionSelector != null) {
        //versionSelector.setProjectId(projectId);
        //}

        DefaultMutableTreeNode testCasesRoot = new DefaultMutableTreeNode("Test Cases");
        DefaultMutableTreeNode testCasesNode = DirectoryMapper.buildTestCasesSubTree(project);
        testCasesRoot.add(testCasesNode);

        DirectoryMapper.testCasesTreeModel = new DefaultTreeModel(testCasesRoot);
        testCaseTree.setModel(DirectoryMapper.testCasesTreeModel);
        testCaseTree.setRootVisible(false);

        DefaultMutableTreeNode testPlansRoot = new DefaultMutableTreeNode("Test Plans");
        DefaultMutableTreeNode testPlansNode = DirectoryMapper.buildTestPlansSubTree(project);
        testPlansRoot.add(testPlansNode);

        DirectoryMapper.testPlansTreeModel = new DefaultTreeModel(testPlansRoot);
        testPlanTree.setModel(DirectoryMapper.testPlansTreeModel);
        testPlanTree.setRootVisible(false);
    }

    static class IntelliJRenderer extends SimpleColoredComponent implements TreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            //System.out.println("Panel.getTreeCellRendererComponent()");
            this.clear(); // مسح الحالة السابقة (ضروري جداً)

            Object userObject = null;
            if (value instanceof DefaultMutableTreeNode node) {
                userObject = node.getUserObject();
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
                if (projectNode.getId() != null && Shortcuts.isCutNode(projectNode.getId())) {
                    style = SimpleTextAttributes.GRAYED_ATTRIBUTES;
                }
                append(projectNode.getName(), style); // ✅ إضافة النص
            }

            // 2. حالة الـ TestPlan
            else if (userObject instanceof TestPlan plan) {
                //Icon planIcon = (plan.getType() == 1) ? AllIcons.Nodes.Artifact : AllIcons.Nodes.Folder;
                Icon planIcon = AllIcons.Nodes.Artifact;
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