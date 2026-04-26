package testGit.projectPanel.projectSelector;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import lombok.Setter;
import testGit.pojo.Config;
import testGit.pojo.DirectoryMapper;
import testGit.pojo.dto.dirs.TestProjectDirectoryDto;
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
    private DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList;

    @Getter
    @Setter
    private ComboBox<TestProjectDirectoryDto> selectedTestProject;

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
                            return !name.startsWith(".");
                        })
                        .peek(System.out::println)
                        .map(DirectoryMapper::testProjectNode)
                        .filter(Objects::nonNull)
                        //.filter(p -> p.getProjectStatus() == ProjectStatus.ACTIVE)
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
        TestProjectDirectoryDto firstTestTestProjectDirectory = testProjectList.getElementAt(0);
        selectedTestProject.setSelectedItem(firstTestTestProjectDirectory);
        //filterByTestProject(firstTestProject);
        return true;
    }

    public void addTestProject(TestProjectDirectoryDto newTestTestProjectDirectory) {
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

    public void removeTestProject(SimpleTree tree, TestProjectDirectoryDto testProjectDirectory) {
        int indexToRemove = -1;
        for (int i = 0; i < testProjectList.getSize(); i++) {
            if (testProjectList.getElementAt(i).getName().equals(testProjectDirectory.getName())) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove == -1) return;

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

    public void filterByTestProject(TestProjectDirectoryDto testProjectDirectory) {
        System.out.println("Panel.filterByProject(): " + testProjectDirectory.getName());

        projectPanel.getTestCaseTreeBuilder().buildTree(selectedTestProject.getItem());
        projectPanel.getTestRunTreeBuilder().buildTree(selectedTestProject.getItem());

    }

}