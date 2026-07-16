package org.testin.projectPanel.projectSelector;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.testin.pojo.DirectoryMapper;
import org.testin.pojo.DirectoryType;
import org.testin.pojo.ProjectStatus;
import org.testin.pojo.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.settings.Setting;
import org.testin.util.indexer.ProjectIndexer;
import org.testin.util.logger.Log;
import org.testin.util.services.Services;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TestProjectSelector {
    private static final String SELECTED_PROJECT_KEY = "org.testin.selectedTestProject";

    private final Project project;
    private final ProjectPanel projectPanel;

    @Getter
    private boolean isLoading = false;

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
        selectedTestProject.addActionListener(e -> {
            if (isLoading) return;

            TestProjectDirectoryDto selected = (TestProjectDirectoryDto) selectedTestProject.getSelectedItem();
            if (selected != null)
                PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, selected.getName());
        });
    }

    public boolean init() {
        Log.info("TestProjectSelector.init()");
        return loadTestProjectList();
    }

    public boolean loadTestProjectList() {
        Log.info("TestProjectSelector.loadTestProjectList()");

        // Save the currently selected project name before clearing the list
        // so after reload we can re-select it (e.g. after create/remove)
        final Object currentSelected = selectedTestProject.getSelectedItem();
        final String currentSelectedName = currentSelected instanceof TestProjectDirectoryDto
                ? ((TestProjectDirectoryDto) currentSelected).getName()
                : null;

        isLoading = true;
        TestProjectDirectoryDto projectToSelect;
        try {
            String savedProjectName = PropertiesComponent.getInstance(project).getValue(SELECTED_PROJECT_KEY);

            testProjectList.removeAllElements();
            final Path root = Services.getInstance(project, Setting.class).getTestinPath();

            if (Files.exists(root) && Files.isDirectory(root)) {
                final ProjectIndexer indexer = Services.getInstance(project, ProjectIndexer.class);

                if (indexer.isIndexed()) {
                    final List<TestProjectDirectoryDto> projects = new ArrayList<>(indexer.getTestProjectsByPath().values());
                    projects.sort(Comparator.comparing(TestProjectDirectoryDto::getName));
                    projects.stream()
                            .filter(tp -> tp.getMarker() != null && tp.getMarker().getStatus() != ProjectStatus.ARCHIVED)
                            .forEach(testProjectList::addElement);
                } else {
                    try (Stream<Path> paths = Files.list(root)) {
                        paths.filter(Files::isDirectory)
                                .filter(path -> !path.getFileName().toString().startsWith("."))
                                .filter(path -> Files.exists(path.resolve(DirectoryType.TP.getMarker())))
                                .peek(path -> Log.info(path.getFileName().toString()))
                                .map(path -> Services.getInstance(project, DirectoryMapper.class).readTestProjectNode(project, path))
                                .filter(Objects::nonNull)
                                .filter(tp -> tp.getMarker() != null && tp.getMarker().getStatus() != ProjectStatus.ARCHIVED)
                                .forEach(testProjectList::addElement);
                    } catch (Exception e) {
                        Log.error("Error reading directory: " + e.getMessage());
                    }
                }
            }

            if (!Files.exists(root) || testProjectList.getSize() == 0) {
                projectPanel.showEmptyState();
                selectedTestProject.setEnabled(false);
                return false;
            }

            selectedTestProject.setEnabled(true);

            // Prefer the currently selected project (if still in the list),
            // then fall back to savedProjectName, then first element
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

            // If current selection didn't match and we have a saved project name, try that
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

        if (projectToSelect != null)
            filterByTestProject(projectToSelect);
        return true;
    }

    public void addTestProject(final TestProjectDirectoryDto newTestTestProjectDirectory) {
        Log.info("TestProjectSelector.addTestProject()");
        if (!selectedTestProject.isEnabled())
            projectPanel.showEmptyState();

        isLoading = true;
        try {
            testProjectList.addElement(newTestTestProjectDirectory);
            selectedTestProject.setSelectedItem(newTestTestProjectDirectory);
        } finally {
            isLoading = false;
        }

        PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, newTestTestProjectDirectory.getName());

        if (testProjectList.getSize() == 1) {
            selectedTestProject.setEnabled(true);
            projectPanel.setupMainLayout();
        }
    }

    public void filterByTestProject(final TestProjectDirectoryDto tpDir) {
        try {
            if (tpDir == null) {
                Log.warn("filterByTestProject: Skipping project with null marker");
                return;
            }

            Log.info("Panel.filterByProject(): " + tpDir.getName());

            if (!isLoading)
                PropertiesComponent.getInstance(project).setValue(SELECTED_PROJECT_KEY, tpDir.getName());

            if (tpDir.getMarker().getStatus() == ProjectStatus.ACTIVE) {
                projectPanel.getTestCaseTreeBuilder().buildTree(selectedTestProject.getItem());
                projectPanel.getTestRunTreeBuilder().buildTree(selectedTestProject.getItem());
            } else {
                if (projectPanel.getProjectTree() != null) {
                    projectPanel.getProjectTree().refreshTree();
                }
            }

            if (projectPanel.getBranchSelector() != null) {
                projectPanel.getBranchSelector().updateProject(tpDir);
            }

        } catch (Exception e) {
            Log.error("filterByTestProject: Error for project '" + (tpDir != null ? tpDir.getName() : "null") + "': " + e.getMessage());
        }
    }

}