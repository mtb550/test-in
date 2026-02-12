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
import testGit.pojo.TestPlan;
import testGit.projectPanel.testCaseTab.MouseAdapter;
import testGit.projectPanel.testPlanTab.TestPlanMouseAdapter;
import testGit.util.Directory;
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

        // === Create Scroll Panes ===
        JBScrollPane testCaseScrollPane = new JBScrollPane(testCaseTree);
        JBScrollPane testPlanScrollPane = new JBScrollPane(testPlanTree);

        // === Project Selector Bar ===
        JPanel topBar = new JPanel(new BorderLayout());


        topBar.add(projectSelector.selected(), BorderLayout.NORTH);

        testGit.pojo.Directory selectedProject = ProjectSelector.getSelectedProject();

        // ✅ Assign to field so we can refresh it on project change
        versionSelector = new VersionSelector(selectedProject);
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

        Directory.buildTestCasesTree();
        testCaseTree.setModel(Directory.getTestCasesTreeModel());
        //testCaseTree.setRootVisible(false);
        testCaseTree.setShowsRootHandles(true);
        testCaseTree.setCellRenderer(new IntelliJRenderer());
        testCaseTree.addMouseListener(new MouseAdapter(this));
        Shortcuts.register(testCaseTree, Config.getProject());
        OpenFeatureAction.register(testCaseTree);
        testCaseTree.setDragEnabled(true);
        testCaseTree.setDropMode(DropMode.ON_OR_INSERT);
        testCaseTree.setTransferHandler(new TransferHandler(testCaseTree));
        ShortcutRegistry.Explorer(testCaseTree, this);
    }

    private void setupTestPlanTree() {
        System.out.println("Panel.setupTestPlanTree()");

        Directory.buildTestPlansTree();
        testPlanTree.setModel(Directory.getTestPlansTreeModel());
        //testPlanTree.setRootVisible(false);
        testPlanTree.addMouseListener(new TestPlanMouseAdapter(this));
        testPlanTree.setShowsRootHandles(true);
        testPlanTree.setCellRenderer(new IntelliJRenderer());
        testPlanTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) testPlanTree.getLastSelectedPathComponent();
            //if (selected != null && selected.getUserObject() instanceof TestPlan plan && plan.getType() == 1) {
            // TestPlanEditor.open(plan.getId());
            //}
        });
    }

    public void refreshTestCaseTree() {
        System.out.println("Panel.refreshTestCaseTree()");

        Directory.buildTestCasesTree();
        Directory.buildTestPlansTree();
        testCaseTree.setModel(Directory.getTestCasesTreeModel());
        testPlanTree.setModel(Directory.getTestPlansTreeModel());
    }

    public void refreshTestPlanTree() {
        System.out.println("Panel.refreshTestPlanTree()");

    }

    public void refreshProjects() {
        System.out.println("Panel.refreshProjects()");

    }

    public void filterByProject(final testGit.pojo.Directory project) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        if (project.getName().equals("All Projects")) {
            // 1. إعادة بناء البيانات الشاملة
            Directory.buildTestCasesTree();
            Directory.buildTestPlansTree();

            // 2. تحديث الموديل أولاً
            testCaseTree.setModel(Directory.getTestCasesTreeModel());
            testPlanTree.setModel(Directory.getTestPlansTreeModel());

            // 3. إخفاء الجذر (كلمة TEST CASES)
            testCaseTree.setRootVisible(false);
            testPlanTree.setRootVisible(false);

        } else {
            // 1. بناء العقد للمشروع المختار
            DefaultMutableTreeNode casesRoot = Directory.buildNodeRecursive(project, "testCases");
            DefaultMutableTreeNode plansRoot = Directory.buildNodeRecursive(project, "testPlans");

            // 2. إنشاء الموديلات الجديدة وتحديثها
            Directory.setTestCasesTreeModel(new DefaultTreeModel(casesRoot));
            Directory.setTestPlansTreeModel(new DefaultTreeModel(plansRoot));

            testCaseTree.setModel(Directory.getTestCasesTreeModel());
            testPlanTree.setModel(Directory.getTestPlansTreeModel());

            // 3. إظهار الجذر (ليظهر اسم المشروع في القمة)
            testCaseTree.setRootVisible(true);
            testPlanTree.setRootVisible(true);
        }

        // 4. تحديث الواجهة وتوسيع العقد
        testCaseTree.revalidate();
        testPlanTree.revalidate();
        testCaseTree.repaint();

        // تأكد من تفعيل هذه السطور لضمان فتح المجلدات فوراً
        //expandAllNodes(testCaseTree);
        //expandAllNodes(testPlanTree);
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
            if (userObject instanceof testGit.pojo.Directory dir) {
                // تعيين الأيقونة بناءً على النوع
                Icon icon = AllIcons.Nodes.Folder; // افتراضي
                if (dir.getType() != null) {
                    icon = switch (dir.getType()) {
                        case P -> AllIcons.Nodes.Project;
                        case S -> AllIcons.Nodes.Folder;
                        case F -> AllIcons.Nodes.Class;
                        case TP -> AllIcons.Nodes.WebFolder;
                        case TR -> AllIcons.Nodes.AbstractMethod;
                        default -> AllIcons.Nodes.AbstractException;
                    };
                }
                setIcon(icon); // ✅ تعيين الأيقونة

                // تعيين النص والستايل
                SimpleTextAttributes style = SimpleTextAttributes.REGULAR_ATTRIBUTES;
                if (dir.getFilePath() != null && Shortcuts.isCutNode(dir.getFilePath())) {
                    style = SimpleTextAttributes.GRAYED_ATTRIBUTES;
                }
                append(dir.getName(), style); // ✅ إضافة النص
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