package org.testin.projectPanel.tree;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.testin.enums.ProjectStatus;
import org.testin.mappers.dto.dirs.TestProjectDirectoryDto;
import org.testin.projectPanel.ProjectPanel;
import org.testin.util.logger.Logger;

public class TestCaseTreeBuilder extends AbstractTreeBuilder {

    public TestCaseTreeBuilder(final @NotNull Project project, final ProjectPanel projectPanel) {
        super(project, projectPanel);
    }

    public void buildTree(final TestProjectDirectoryDto selectedTestProjectDirectory) {
        try {
            if (selectedTestProjectDirectory == null || selectedTestProjectDirectory.getMarker().getStatus() != ProjectStatus.ACTIVE) {
                this.rootNode = null;
                ApplicationManager.getApplication().invokeLater(() -> projectPanel.getProjectTree().refreshTree());
                return;
            }

            super.buildTree(selectedTestProjectDirectory.getTestCasesDirectory());

        } catch (final Exception ex) {
            Logger.error("TestCaseTreeBuilder.buildTree() error for directory '" + (selectedTestProjectDirectory != null ? selectedTestProjectDirectory.getName() : "null") + "': " + ex.getMessage());
            this.rootNode = null;
            ApplicationManager.getApplication().invokeLater(() -> projectPanel.getProjectTree().refreshTree());
        }
    }

}