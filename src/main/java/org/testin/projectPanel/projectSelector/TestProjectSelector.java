package org.testin.projectPanel.projectSelector;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Logger;
import org.testin.util.services.Services;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProjectSelector {

    private static final String SELECTED_PROJECT_KEY = "org.testin.selectedTestProject";

    private final @NotNull Project project;
    private final @NotNull ProjectPanel projectPanel;

    @Getter
    private boolean isLoading = false;

    @Getter
    @Setter
    @NotNull
    private DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList;

    @Getter
    @Setter
    @NotNull
    private ComboBox<TestProjectDirectoryDto> selectedTestProject;

    public TestProjectSelector(final @NotNull Project project, final @NotNull ProjectPanel projectPanel) {
        this.project = project;
        this.projectPanel = projectPanel;
        testProjectList = new DefaultComboBoxModel<>();
        selectedTestProject = new ComboBox<>(testProjectList);

        selectedTestProject.setFocusable(false);
        selectedTestProject.setRenderer(new RendererImpl());

        selectedTestProject.addActionListener(new ListenerImpl(projectPanel));
        selectedTestProject.addActionListener(e -> {
            if (isLoading) return;

            TestProjectDirectoryDto selected = (TestProjectDirectoryDto) selectedTestProject.getSelectedItem();
            if (selected != null)
                PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, selected.getName());
        });
    }

    public void init() {
        Logger.info("TestProjectSelector.init()");
        loadTestProjectList();
    }

    public void loadTestProjectList() {
        Logger.info("TestProjectSelector.loadTestProjectList()");

        final Object currentSelected = selectedTestProject.getSelectedItem();
        final String currentSelectedName = currentSelected instanceof TestProjectDirectoryDto ? ((TestProjectDirectoryDto) currentSelected).getName() : null;

        isLoading = true;
        TestProjectDirectoryDto projectToSelect;
        try {
            String savedProjectName = PropertiesComponent.getInstance(project).getValue(SELECTED_PROJECT_KEY);

            testProjectList.removeAllElements();
            final Path root = Services.getInstance(project, Setting.class).getTestinPath();

            if (Files.exists(root) && Files.isDirectory(root)) {
                final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

                final List<TestProjectDirectoryDto> projects = new ArrayList<>(indexer.getTestProjectsByPath().values());
                projects.sort(Comparator.comparing(TestProjectDirectoryDto::getName));
                projects.stream()
                        .filter(tp -> tp.getMarker().getStatus() != ProjectStatus.ARCHIVED)
                        .forEach(testProjectList::addElement);
            }

            if (!Files.exists(root) || testProjectList.getSize() == 0) {
                projectPanel.showEmptyState();
                selectedTestProject.setEnabled(false);
                selectedTestProject.setSelectedItem(null);
                return;
            }

            selectedTestProject.setEnabled(true);

            projectToSelect = testProjectList.getElementAt(0);

            if (currentSelectedName != null) {
                for (int i = 0; i < testProjectList.getSize(); i++) {
                    TestProjectDirectoryDto item = testProjectList.getElementAt(i);
                    if (currentSelectedName.equals(item.getName())) {
                        projectToSelect = item;
                        break;
                    }
                }
            }

            if (savedProjectName != null && !savedProjectName.equals(currentSelectedName)) {
                for (int i = 0; i < testProjectList.getSize(); i++) {
                    TestProjectDirectoryDto item = testProjectList.getElementAt(i);
                    if (savedProjectName.equals(item.getName())) {
                        projectToSelect = item;
                        break;
                    }
                }
            }

            selectedTestProject.setSelectedItem(projectToSelect);

        } finally {
            isLoading = false;
        }

        if (projectToSelect != null) {
            if (projectPanel.getPanel().getComponentCount() == 0) {
                projectPanel.setupMainLayout();
            }
            filterByTestProject(projectToSelect);
        }
    }

    public void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        Logger.info("TestProjectSelector.addTestProject()");
        if (!selectedTestProject.isEnabled())
            projectPanel.showEmptyState();

        isLoading = true;
        try {
            testProjectList.addElement(tp);
            selectedTestProject.setSelectedItem(tp);
        } finally {
            isLoading = false;
        }

        PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, tp.getName());

        if (testProjectList.getSize() == 1) {
            selectedTestProject.setEnabled(true);
        }
    }

    public void filterByTestProject(final @NotNull TestProjectDirectoryDto tp) {
        try {

            Logger.info("Panel.filterByProject(): " + tp.getName());

            if (!isLoading)
                PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, tp.getName());

            if (tp.getMarker().getStatus() == ProjectStatus.ACTIVE) {
                projectPanel.getTestCaseTreeBuilder().buildTree(selectedTestProject.getItem());
                projectPanel.getTestRunTreeBuilder().buildTree(selectedTestProject.getItem());
            } else {
                projectPanel.getProjectTree().refreshTree();
            }

            projectPanel.getBranchSelector().updateProject(tp);

        } catch (final Exception ex) {
            Logger.error("filterByTestProject: Error for project '" + tp.getName() + "': " + ex.getMessage());
        }
    }

}