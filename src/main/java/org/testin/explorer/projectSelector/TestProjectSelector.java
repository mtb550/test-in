package org.testin.explorer.projectSelector;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.enums.ProjectStatus;
import org.testin.explorer.ExplorerPanel;
import org.testin.indexer.ProjectIndexer;
import org.testin.logger.Logger;
import org.testin.model.dto.dirs.TestProjectDirectoryDto;
import org.testin.services.Services;

import javax.swing.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProjectSelector {

    private static final String SELECTED_PROJECT_KEY = "org.testin.selectedTestProject";

    private final @NotNull Project p;
    private final @NotNull ExplorerPanel pp;
    @Getter
    private final @NotNull DefaultComboBoxModel<TestProjectDirectoryDto> testProjectList;
    @Getter
    private final @NotNull ComboBox<TestProjectDirectoryDto> selectedTestProject;
    @Getter
    private boolean isLoading = false;

    public TestProjectSelector(final @NotNull Project p, final @NotNull ExplorerPanel pp) {
        this.p = p;
        this.pp = pp;
        testProjectList = new DefaultComboBoxModel<>();
        selectedTestProject = new ComboBox<>(testProjectList);

        selectedTestProject.setFocusable(false);
        selectedTestProject.setRenderer(new TestProjectRenderer());

        selectedTestProject.addActionListener(new ProjectSelectionListener(pp));
        selectedTestProject.addActionListener(e -> {
            if (isLoading) return;

            final TestProjectDirectoryDto selected = (TestProjectDirectoryDto) selectedTestProject.getSelectedItem();
            if (selected != null)
                PropertiesComponent.getInstance(p).setValue(SELECTED_PROJECT_KEY, selected.getName());
        });
    }

    public void init() {
        Logger.info("TestProjectSelector.init()");
        loadTestProjectList();
    }

    public void loadTestProjectList() {
        Logger.info("TestProjectSelector.loadTestProjectList()");

        final Object currentSelected = selectedTestProject.getSelectedItem();
        final @Nullable String currentSelectedName = currentSelected instanceof TestProjectDirectoryDto selectedProject
                ? selectedProject.getName()
                : null;

        isLoading = true;
        final TestProjectDirectoryDto projectToSelect;
        try {
            final @Nullable String savedProjectName = PropertiesComponent.getInstance(p).getValue(SELECTED_PROJECT_KEY);

            testProjectList.removeAllElements();
            final ProjectIndexer indexer = Services.getInstance(p, ProjectIndexer.class);

            if (indexer.rootExists()) {
                final List<TestProjectDirectoryDto> projects = new ArrayList<>(indexer.getTestProjectsByPath().values());
                projects.sort(Comparator.comparing(TestProjectDirectoryDto::getName));
                projects.stream()
                        .filter(tp -> tp.getMarker().getStatus() != ProjectStatus.ARCHIVED)
                        .forEach(testProjectList::addElement);
            }

            if (!indexer.rootExists() || testProjectList.getSize() == 0) {
                pp.showEmptyState();
                selectedTestProject.setEnabled(false);
                selectedTestProject.setSelectedItem(null);
                return;
            }

            selectedTestProject.setEnabled(true);

            // The name the combo already shows wins; a saved name from a previous
            // session only applies when it names a different project.
            final TestProjectDirectoryDto restored = savedProjectName != null && !savedProjectName.equals(currentSelectedName)
                    ? findByName(savedProjectName)
                    : null;
            final TestProjectDirectoryDto current = findByName(currentSelectedName);

            projectToSelect = restored != null ? restored
                    : current != null ? current
                    : testProjectList.getElementAt(0);

            selectedTestProject.setSelectedItem(projectToSelect);

        } finally {
            isLoading = false;
        }

        if (pp.getPanel().getComponentCount() == 0) {
            pp.setupMainLayout();
        }
        filterByTestProject(projectToSelect);
    }

    private @Nullable TestProjectDirectoryDto findByName(final @Nullable String name) {
        if (name == null) return null;

        for (int i = 0; i < testProjectList.getSize(); i++) {
            final TestProjectDirectoryDto item = testProjectList.getElementAt(i);
            if (name.equals(item.getName())) return item;
        }
        return null;
    }

    public void addTestProject(final @NotNull TestProjectDirectoryDto tp) {
        Logger.info("TestProjectSelector.addTestProject()");
        if (!selectedTestProject.isEnabled())
            pp.showEmptyState();

        isLoading = true;
        try {
            testProjectList.addElement(tp);
            selectedTestProject.setSelectedItem(tp);
        } finally {
            isLoading = false;
        }

        PropertiesComponent.getInstance(p).setValue(SELECTED_PROJECT_KEY, tp.getName());

        if (testProjectList.getSize() == 1) {
            selectedTestProject.setEnabled(true);
        }
    }

    public void filterByTestProject(final @NotNull TestProjectDirectoryDto tp) {
        try {

            Logger.info("Panel.filterByProject(): " + tp.getName());

            if (!isLoading)
                PropertiesComponent.getInstance(p).setValue(SELECTED_PROJECT_KEY, tp.getName());

            pp.getProjectTree().refresh();

            pp.getBranchSelector().updateProject(tp);

        } catch (final Exception ex) {
            Logger.error("filterByTestProject: Error for project '" + tp.getName() + "': " + ex.getMessage());
        }
    }

}
