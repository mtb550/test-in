package testGit.projectPanel.projectSelector;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
import testGit.projectPanel.testCaseTab.TestCaseRenderer;
import testGit.util.TestCasesDirectoryMapper;
import testGit.util.TestRunsDirectoryMapper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ProjectSelector {
    public static ComboBox<Directory> comboBox;
    private final DefaultComboBoxModel<Directory> comboBoxModel;
    public ProjectPanel projectPanel;

    public ProjectSelector(final ProjectPanel projectPanel) {
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

        File root = Config.getRootFolderFile();

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

    public void filterByProject(final Directory project, ProjectPanel projectPanel) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        if (project.getName().equals("All Projects")) {
            TestCasesDirectoryMapper.buildTreeAsync(projectPanel.getTestCaseTree());
            TestRunsDirectoryMapper.buildTreeAsync(projectPanel.getTestRunTree());
        } else {
            DefaultMutableTreeNode casesRoot = TestCasesDirectoryMapper.buildNodeRecursive(project, "testCases");
            DefaultMutableTreeNode runsRoot = TestRunsDirectoryMapper.buildNodeRecursive(project, "testRuns");

            projectPanel.getTestCaseTree().setModel(new DefaultTreeModel(casesRoot));
            projectPanel.getTestRunTree().setModel(new DefaultTreeModel(runsRoot));
        }

        if (projectPanel.getTestCaseTree().getCellRenderer() == null ||
                !(projectPanel.getTestCaseTree().getCellRenderer() instanceof TestCaseRenderer)) {
            projectPanel.setupTestCaseTree(Config.getProject());
        }

        projectPanel.getTestCaseTree().setRootVisible(true);
        projectPanel.getTestRunTree().setRootVisible(true);

        projectPanel.getTestCaseTree().repaint();
        projectPanel.getTestRunTree().repaint();
    }

    public void onProjectListLoaded(List<Directory> projects) {
        ApplicationManager.getApplication().invokeLater(() -> {
            comboBoxModel.removeAllElements();

            Directory allProjects = new Directory().setName("All Projects");
            comboBoxModel.addElement(allProjects);

            for (Directory p : projects) {
                comboBoxModel.addElement(p);
            }

            if (comboBoxModel.getSize() > 0) {
                comboBox.setSelectedIndex(0);
            }

            if (projectPanel != null && Config.getProject() != null) {
                projectPanel.setupTestCaseTree(Config.getProject());
                projectPanel.setupTestRunTree(Config.getProject());
            }
        });
    }

}