package org.testin.projectPanel.projectSelector;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.treeStructure.SimpleTree;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class TestProjectSelector {
    private final Project project;
    private final ProjectPanel projectPanel;

    @Getter
    @Setter
    private DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList;

    @Getter
    @Setter
    private ComboBox<TestProjectDirectoryDto> selectedTestProject;

    public TestProjectSelector(final @NotNull Project project, final ProjectPanel projectPanel) {
        this.project = project;
        this.projectPanel = projectPanel;
        testProjectList = new DefaultComboBoxModel<>();
        selectedTestProject = new ComboBox<>(testProjectList);

        selectedTestProject.setFocusable(false);
        selectedTestProject.setRenderer(new RendererImpl());
        selectedTestProject.addActionListener(new ListenerImpl(projectPanel));
    }

    public boolean init() {
        Log.info("TestProjectSelector.init()");
        return loadTestProjectList();
    }

    public boolean loadTestProjectList() {
        Log.info("TestProjectSelector.loadTestProjectList()");

        testProjectList.removeAllElements();

        final Path root = Services.getInstance(project, Setting.class).getTestinPath();

        if (Files.exists(root) && Files.isDirectory(root)) {

            try (Stream<Path> paths = Files.list(root)) {

                paths.filter(Files::isDirectory)
                        .filter(path -> !path.getFileName().toString().startsWith("."))
                        .filter(path -> Files.exists(path.resolve(DirectoryType.TP.getMarker())))
                        .peek(path -> Log.info(path.getFileName().toString()))
                        .map(path -> Services.getInstance(project, DirectoryMapper.class).readTestProjectNode(project, path))
                        .filter(Objects::nonNull)
                        .forEach(testProjectList::addElement);

            } catch (Exception e) {
                Log.error("Error reading directory: " + e.getMessage());
                Log.error("Exception: " + e.getMessage());
            }
        }

        if (!Files.exists(root) || testProjectList.getSize() == 0) {
            Log.info("no projects. " + Files.exists(root) + " , " + testProjectList.getSize());
            projectPanel.showEmptyState();
            selectedTestProject.setEnabled(false);
            return false;
        }

        selectedTestProject.setEnabled(true);
        TestProjectDirectoryDto firstTestTestProjectDirectory = testProjectList.getElementAt(0);
        selectedTestProject.setSelectedItem(firstTestTestProjectDirectory);
        return true;
    }

    public void addTestProject(final TestProjectDirectoryDto newTestTestProjectDirectory) {
        Log.info("TestProjectSelector.addTestProject()");
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

    public void removeTestProject(final SimpleTree tree, final TestProjectDirectoryDto testProjectDirectory) {
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

    public void filterByTestProject(final TestProjectDirectoryDto tpDir) {
        Log.info("Panel.filterByProject(): " + tpDir.getName());

        if (tpDir.getMarker().getStatus() == ProjectStatus.ACTIVE) {
            projectPanel.getTestCaseTreeBuilder().buildTree(selectedTestProject.getItem());
            projectPanel.getTestRunTreeBuilder().buildTree(selectedTestProject.getItem());
        } else {
            if (projectPanel.getProjectTree() != null) {
                projectPanel.getProjectTree().updateNodes();
            }
        }

        if (projectPanel.getBranchSelector() != null) {
            projectPanel.getBranchSelector().updateProject(tpDir);
        }
    }

}