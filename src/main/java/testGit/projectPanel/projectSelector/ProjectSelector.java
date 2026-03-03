package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.testCaseTab.TestCaseRenderer;
import testGit.projectPanel.testRunTab.TestRunRenderer;
import testGit.util.TestCasesDirectoryMapper;
import testGit.util.TestRunsDirectoryMapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;

public class ProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> comboBoxModel;
    public ProjectPanel projectPanel;

    public ProjectSelector(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.comboBoxModel = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(comboBoxModel);
        comboBox.setFocusable(false);

        comboBox.setRenderer(new Renderer());
        comboBox.addActionListener(new Listener(projectPanel));

        //loadProjectList();  moved to post activity
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    public void loadProjectList() {
        System.out.println("ComboBoxProjectSelector.loadProjectList()");

        comboBoxModel.removeAllElements();

        Directory allProjects = new Directory().setName("All Projects");
        comboBoxModel.addElement(allProjects);

        File root = Config.getTestGitPath().toFile();

        if (root == null || !root.exists()) {
            System.out.println("TestGit: Root folder not ready or doesn't exist. Showing default only.");
            comboBox.setSelectedIndex(0);
            comboBox.setEnabled(true);
            return;
        }

        File[] dirs = root.listFiles(File::isDirectory);

        if (dirs != null) {
            Arrays.stream(dirs)
                    .filter(dir -> !dir.getName().equals(".git") && dir.getName().contains("_"))
                    .map(TestCasesDirectoryMapper::map)
                    .filter(p -> p != null && p.getActive() == 1)
                    .forEach(comboBoxModel::addElement);
        }

        comboBox.setEnabled(comboBoxModel.getSize() > 0);
        comboBox.setSelectedIndex(0);
    }

    public JComboBox<Directory> selected() {
        return comboBox;
    }

    public void selectProject(@NotNull final Directory project) {
        System.out.println("ComboBoxProjectSelector.selectProject()");
        comboBox.setSelectedItem(project);
    }

    public void addProject(@NotNull final Directory project) {
        System.out.println("ComboBoxProjectSelector.addProject()");

        if (!comboBox.isEnabled()) {
            comboBox.setEnabled(true);
        }

        comboBoxModel.addElement(project);
    }

    public void filterByProject(Directory project, ProjectPanel projectPanel) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        if (project.getName().equals("All Projects")) {
            TestCasesDirectoryMapper.buildTreeAsync(projectPanel.getTestCaseTabController().getTree());
            TestRunsDirectoryMapper.buildTreeAsync(projectPanel.getTestRunTabController().getTree());
        } else {
            DefaultMutableTreeNode casesRoot = TestCasesDirectoryMapper.buildNodeRecursive(project, "testCases");
            DefaultMutableTreeNode runsRoot = TestRunsDirectoryMapper.buildNodeRecursive(project, "testRuns");

            projectPanel.getTestCaseTabController().getTree().setModel(new DefaultTreeModel(casesRoot));
            projectPanel.getTestRunTabController().getTree().setModel(new DefaultTreeModel(runsRoot));
        }

        if (projectPanel.getTestCaseTabController().getTree().getCellRenderer() == null ||
                !(projectPanel.getTestCaseTabController().getTree().getCellRenderer() instanceof TestCaseRenderer)) {
            projectPanel.getTestCaseTabController().setup();
        }

        if (projectPanel.getTestRunTabController().getTree().getCellRenderer() == null ||
                !(projectPanel.getTestRunTabController().getTree().getCellRenderer() instanceof TestRunRenderer)) {
            projectPanel.getTestRunTabController().setup();
        }

        projectPanel.getTestCaseTabController().getTree().setRootVisible(true);
        projectPanel.getTestRunTabController().getTree().setRootVisible(true);

        projectPanel.getTestCaseTabController().getTree().repaint();
        projectPanel.getTestRunTabController().getTree().repaint();
    }

}