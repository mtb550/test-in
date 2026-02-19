package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import testGit.pojo.Config;
import testGit.pojo.Directory;
import testGit.projectPanel.ProjectPanel;
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

    public ProjectSelector(final ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        this.comboBoxModel = new DefaultComboBoxModel<>();
        comboBox = new ComboBox<>(comboBoxModel);
        comboBox.setFocusable(false);

        comboBox.setRenderer(new Renderer());
        comboBox.addActionListener(new Listener(projectPanel));

        loadProjectList();
    }

    public static Directory getSelectedProject() {
        return (Directory) comboBox.getSelectedItem();
    }

    public void loadProjectList() {
        System.out.println("ComboBoxProjectSelector.loadProjects()");

        comboBoxModel.removeAllElements();

        File root = Config.getRootFolderFile();
        File[] dirs = root.listFiles(File::isDirectory);

        Directory allProjects = new Directory().setName("All Projects");
        comboBoxModel.addElement(allProjects);

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

    public void filterByProject(final Directory project) {
        System.out.println("Panel.filterByProject(): " + project.getName());

        if (project.getName().equals("All Projects")) {
            TestCasesDirectoryMapper.buildTree();
            TestRunsDirectoryMapper.buildTree();

            projectPanel.getTestCaseTree().setModel(TestCasesDirectoryMapper.getTreeModel());
            projectPanel.getTestRunTree().setModel(TestRunsDirectoryMapper.getTreeModel());

            projectPanel.getTestCaseTree().setRootVisible(true);
            projectPanel.getTestRunTree().setRootVisible(true);

        } else {
            DefaultMutableTreeNode casesRoot = TestCasesDirectoryMapper.buildNodeRecursive(project, "testCases");
            DefaultMutableTreeNode runsRoot = TestRunsDirectoryMapper.buildNodeRecursive(project, "testRuns");

            TestCasesDirectoryMapper.setTreeModel(new DefaultTreeModel(casesRoot));
            TestRunsDirectoryMapper.setTreeModel(new DefaultTreeModel(runsRoot));

            projectPanel.getTestCaseTree().setModel(TestCasesDirectoryMapper.getTreeModel());
            projectPanel.getTestRunTree().setModel(TestRunsDirectoryMapper.getTreeModel());

            projectPanel.getTestCaseTree().setRootVisible(true);
            projectPanel.getTestRunTree().setRootVisible(true);
        }

        projectPanel.getTestCaseTree().revalidate();
        projectPanel.getTestRunTree().revalidate();

        projectPanel.getTestCaseTree().repaint();
        projectPanel.getTestRunTree().repaint();

        //expandAllNodes(testCaseTree);
        //expandAllNodes(testRunTree);
    }

}