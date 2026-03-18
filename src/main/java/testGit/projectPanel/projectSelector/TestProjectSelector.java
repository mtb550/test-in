package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.Config;
import testGit.pojo.ProjectStatus;
import testGit.pojo.tree.dirs.TestProjectDirectory;
import testGit.pojo.tree.mappers.TestProjectMapper;
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
    private DefaultComboBoxModel<TestProjectDirectory> testProjectList;

    @Getter
    @Setter
    private ComboBox<TestProjectDirectory> selectedTestProject;

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
        return loadTestProjectList();
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
        TestProjectDirectory firstTestTestProjectDirectory = testProjectList.getElementAt(0);
        selectedTestProject.setSelectedItem(firstTestTestProjectDirectory);
        //filterByTestProject(firstTestProject);
        return true;
    }

    public void addTestProject(TestProjectDirectory newTestTestProjectDirectory) {
        System.out.println("TestProjectSelector.addTestProject()");
        if (!selectedTestProject.isEnabled()) {
            projectPanel.showEmptyState();
        }

        testProjectList.addElement(newTestTestProjectDirectory);
        selectedTestProject.setSelectedItem(newTestTestProjectDirectory);
        if (testProjectList.getSize() == 1) {
            selectedTestProject.setEnabled(true);
            projectPanel.setupMainLayout();
        }
    }

    public void removeTestProject(SimpleTree tree, TestProjectDirectory testProjectDirectory) {
        int indexToRemove = -1;
        for (int i = 0; i < testProjectList.getSize(); i++) {
            if (testProjectList.getElementAt(i).getName().equals(testProjectDirectory.getName())) {
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

    public void filterByTestProject(TestProjectDirectory testProjectDirectory) {
        System.out.println("Panel.filterByProject(): " + testProjectDirectory.getName());

        projectPanel.getTestCaseTreeBuilder().buildTree(selectedTestProject.getItem());
        projectPanel.getTestRunTreeBuilder().buildTree(selectedTestProject.getItem());

    }

}