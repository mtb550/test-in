package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.Config;
import testGit.pojo.ProjectStatus;
import testGit.pojo.TestProject;
import testGit.pojo.mappers.TestProjectMapper;
import testGit.projectPanel.ProjectPanel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TestProjectSelector {
    private final ProjectPanel projectPanel;
    @Getter
    @Setter
    private DefaultComboBoxModel<TestProject> testProjectList;
    @Getter
    @Setter
    private ComboBox<TestProject> selectedTestProject;

    public TestProjectSelector(ProjectPanel projectPanel) {
        this.projectPanel = projectPanel;
        testProjectList = new DefaultComboBoxModel<>();
        selectedTestProject = new ComboBox<>(testProjectList);

        selectedTestProject.setFocusable(false);
        selectedTestProject.setRenderer(new RendererImpl());
        selectedTestProject.addActionListener(new Listener(projectPanel));
    }

    public boolean init() {
        System.out.println("TestProjectSelector.init()");
        boolean status = loadTestProjectList();
        System.out.println("status = " + status);

        return status;
    }

    public boolean loadTestProjectList() {
        System.out.println("TestProjectSelector.loadTestProjectList()");

        testProjectList.removeAllElements();

        Path root = Config.getTestGitPath();

        if (Files.exists(root) && Files.isDirectory(root)) {

            try (java.util.stream.Stream<Path> paths = java.nio.file.Files.list(root)) {

                paths.filter(java.nio.file.Files::isDirectory)
                        .filter(path -> {
                            String name = path.getFileName().toString();
                            return !name.startsWith(".") && name.contains("_");
                        })
                        .peek(System.out::println)
                        .map(TestProjectMapper::map)
                        .filter(Objects::nonNull)
                        .filter(p -> p.getProjectStatus() == ProjectStatus.AC)
                        .forEach(testProjectList::addElement);

            } catch (Exception e) {
                System.err.println("Error reading directory: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        if (!Files.exists(root) || testProjectList.getSize() == 0) {
            System.out.println("no projects. " + Files.exists(root) + " , " + testProjectList.getSize());
            projectPanel.showEmptyState();
            selectedTestProject.setEnabled(false);
            return false;
        }

        selectedTestProject.setEnabled(true);
        TestProject firstTestTestProject = testProjectList.getElementAt(0);
        selectedTestProject.setSelectedItem(firstTestTestProject);
        //filterByTestProject(firstTestProject);
        return true;
    }

    public void addTestProject(TestProject newTestTestProject) {
        System.out.println("TestProjectSelector.addTestProject()");
        if (!selectedTestProject.isEnabled()) {
            projectPanel.showEmptyState();
        }

        testProjectList.addElement(newTestTestProject);
        selectedTestProject.setSelectedItem(newTestTestProject);
        if (testProjectList.getSize() == 1) {
            selectedTestProject.setEnabled(true);
            projectPanel.setupMainLayout();
        }
    }

    public void removeTestProject(SimpleTree tree, TestProject testProject) {
        int indexToRemove = -1;
        for (int i = 0; i < testProjectList.getSize(); i++) {
            if (testProjectList.getElementAt(i).getName().equals(testProject.getName())) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove == -1) return; // Not found

        ActionListener[] listeners = selectedTestProject.getActionListeners();
        for (ActionListener l : listeners) selectedTestProject.removeActionListener(l);

        try {
            testProjectList.removeElementAt(indexToRemove);

            if (testProjectList.getSize() == 0) {
                testProjectList.removeAllElements();
                projectPanel.showEmptyState();
            } else {
                selectedTestProject.setSelectedItem(testProjectList.getElementAt(Math.max(0, indexToRemove - 1)));
                filterByTestProject(selectedTestProject.getItem());
            }
        } finally {
            for (ActionListener l : listeners) selectedTestProject.addActionListener(l);
        }

        tree.revalidate();
        tree.repaint();
    }

    public void filterByTestProject(TestProject testProject) {
        System.out.println("Panel.filterByProject(): " + testProject.getName());

        projectPanel.getTestCaseTabController().buildTreeAsync(selectedTestProject.getItem());
        projectPanel.getTestRunTabController().buildTreeAsync(selectedTestProject.getItem());

    }

}